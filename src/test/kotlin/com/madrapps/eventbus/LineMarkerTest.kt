package com.madrapps.eventbus

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import junit.framework.TestCase
import java.io.File

class LineMarkerTest : LightCodeInsightFixtureTestCase() {

//    fun test() {
//        TestCase.assertTrue(true)
//        //myFixture.copyDirectoryToProject("", "main/")
//        myFixture.configureByFiles(
//            "java/com/madrapps/eventbus/type/JavaChildType.java",
//            "java/com/madrapps/eventbus/type/JavaType.java",
//            "java/com/madrapps/eventbus/JavaMain.java",
//            "java/com/madrapps/eventbus/JavaObject.java",
//            "java/com/madrapps/eventbus/JavaToKotlin.java",
//            "kotlin/com/madrapps/eventbus/type/KotlinType.kt",
//            "kotlin/com/madrapps/eventbus/KotlinMain.kt",
//            "kotlin/com/madrapps/eventbus/KotlinObject.kt",
//            "kotlin/com/madrapps/eventbus/KotlinToJava.kt"
//        )
//        val findAllGutters = myFixture.findAllGutters()
//        val project = myFixture.project
//        TestCase.assertEquals(3, findAllGutters.size)
//    }

    fun test1() {
        myFixture.configureByText(
            "Sample.java", "package com.madrapps.eventbus;\n" +
                    "\n" +
                    "import org.greenrobot.eventbus.EventBus;\n" +
                    "\n" +
                    "public class Sample {\n" +
                    "    \n" +
                    "    public void something() {\n" +
                    "        EventBus.getDefault().post(new Object());\n" +
                    "    }\n" +
                    "}\n"
        )
        val findAllGutters = myFixture.findAllGutters()
        TestCase.assertEquals(3, findAllGutters.size)
    }

//    override fun getTestDataPath(): String = File("src/test", "project").path
}

