// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.utils.rules

import com.intellij.ide.util.projectWizard.EmptyModuleBuilder
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.ModuleFixture
import com.intellij.testFramework.fixtures.TestFixtureBuilder
import com.intellij.testFramework.fixtures.impl.ModuleFixtureBuilderImpl
import com.intellij.testFramework.fixtures.impl.ModuleFixtureImpl
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.xdebugger.XDebuggerUtil
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

/**
 * JUnit test Rule that will create a Light [Project] and [CodeInsightTestFixture] with Python support. Projects are
 * lazily created and are torn down after each test.
 *
 * If you wish to have just a [Project], you may use Intellij's [com.intellij.testFramework.ProjectRule]
 */
class PythonCodeInsightTestFixtureRule : CodeInsightTestFixtureRule() {
    override fun createTestFixture(): CodeInsightTestFixture {
        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        fixtureFactory.registerFixtureBuilder(
            PythonModuleFixtureBuilder::class.java,
            PythonModuleFixtureBuilder::class.java
        )
        val fixtureBuilder = fixtureFactory.createFixtureBuilder(testName)
        fixtureBuilder.addModule(PythonModuleFixtureBuilder::class.java)
        val newFixture = fixtureFactory.createCodeInsightFixture(fixtureBuilder.fixture)
        newFixture.testDataPath = testDataPath
        newFixture.setUp()

        val module = newFixture.module

        val projectRoot = newFixture.tempDirFixture.getFile(".")!!

        if (SystemInfo.isUnix) {
            val path = Paths.get(projectRoot.path)

            // TODO: Investigate this more. On 2020.1 this folder has strict permissions
            // on code build (due to root?) that prevents it from mounting into docker for sam
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE
                )
            )
        }

        PsiTestUtil.addContentRoot(module, projectRoot)
        val sdk = PyTestSdk.create("3.12.0")
        setModuleSdk(module, sdk)

        return newFixture
    }

    override val fixture: CodeInsightTestFixture
        get() = lazyFixture.value

    fun setModuleSdk(module: Module, sdk: Sdk) {
        runWriteActionAndWait {
            ProjectJdkTable.getInstance().addJdk(sdk, module)
            ModuleRootModificationUtil.setModuleSdk(module, sdk)
        }
    }
}

internal class PythonModuleFixtureBuilder(fixtureBuilder: TestFixtureBuilder<out IdeaProjectTestFixture>) :
    ModuleFixtureBuilderImpl<ModuleFixture>(PlatformPythonModuleType(), fixtureBuilder),
    ModuleFixtureBuilder<ModuleFixture> {

    override fun instantiateFixture(): ModuleFixture = ModuleFixtureImpl(this)
}

internal class PlatformPythonModuleType : PythonModuleTypeBase<EmptyModuleBuilder>() {
    override fun createModuleBuilder(): EmptyModuleBuilder = object : EmptyModuleBuilder() {
        override fun getModuleType(): ModuleType<EmptyModuleBuilder> = instance
    }

    companion object {
        val instance: PlatformPythonModuleType
            get() = ModuleTypeManager.getInstance().findByID("PYTHON_MODULE") as PlatformPythonModuleType
    }
}

class PyTestSdk private constructor(private val version: LanguageLevel) : ProjectJdkImpl("PySdk $version", PyTestSdkType(version)) {
    override fun getVersionString(): String = version.toString()

    override fun getSdkAdditionalData(): SdkAdditionalData = PythonSdkAdditionalData(PyFlavorAndData(PyFlavorData.Empty, FakeCPython(version)))

    companion object {
        fun create(version: String): PyTestSdk {
            val level = LanguageLevel.fromPythonVersion(version) ?: error("Invalid version")
            val sdk = PyTestSdk(level)

            sdk.putUserData(Key.create("MOCK_PY_MARKER_KEY"), true)

            return sdk
        }
    }
}

fun PythonCodeInsightTestFixtureRule.addBreakpoint() {
    runInEdtAndWait {
        val document = fixture.editor.document
        val lambdaClass = fixture.file as PyFile
        val lambdaBody = lambdaClass.topLevelFunctions[0].statementList.statements[0]
        val lineNumber = document.getLineNumber(lambdaBody.textOffset)

        XDebuggerUtil.getInstance().toggleLineBreakpoint(
            project,
            fixture.file.virtualFile,
            lineNumber
        )
    }
}
