package com.san4o.just4fun.android.strings.merge

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

fun main(args: Array<String>) {

    var addPath = ""
    var basePath = ""
    println("Parse arguments : ${args.asList()} ")
    for (i in 0 until args.size) {
        val arg = args[i]

        val split = arg.split("=")
        val name = split[0]
        val value = split[1]

        when (name) {
            "add" -> addPath = value
            "base" -> basePath = value
        }
    }
    if (addPath.isEmpty()) {
       addPath = input("Input ADD strings.xml path")
    }
    val addFile = File(addPath)
    checkFile(addFile)

    if (basePath.isEmpty()) {
        basePath = input("Input BASE strings.xml path")
    }
    val baseFile = File(basePath)
    checkFile(baseFile)

    println("Start merge \nbase : $basePath\nadd : $addPath")

    val addProperties = parse(readXml(addFile))
    val notContainNames = ArrayList(addProperties.keys)

    val newBaseFileContent = FileContentBuilder()

    val readLines = baseFile.readLines()
    for (readLine in readLines) {

        val trimmed = readLine.trim()
        if (!trimmed.startsWith("<string name=")) {
            newBaseFileContent.appendLine(readLine)
            continue
        }

        val startChar = "name=\""
        val name = trimmed.substringAfter(startChar).substringBefore("\"")
        val value = trimmed.substringAfter('>').substringBefore('<')

        if (!addProperties.containsKey(name)) {
            newBaseFileContent.appendLine(readLine)
            continue
        }

        val addValue = addProperties[name]
        if (addValue == value) {
            newBaseFileContent.appendLine(readLine)
            notContainNames.remove(name)
            continue
        }

        val newLine = readLine.replace(">$value<", ">$addValue<")

        if (!newLine.contains(addValue!!)) {
            throw RuntimeException("Error replace $readLine\nreplace: $addValue")
        }
        notContainNames.remove(name)

        println("replace [$name] $value << $addValue")

        newBaseFileContent.appendLine(newLine)
    }

    if (notContainNames.isNotEmpty()){
        println("In base strings.xml NOT CONTAIN : ${notContainNames.joinToString()}")
        val input = input("continue?(y/n)")
        if (input == "n"){
            System.exit(-1)
            return
        }
    }

    baseFile.writeText(newBaseFileContent.build())

    println("merge complete!")



}

fun input(text: String): String {
    val input = Scanner(System.`in`)
    print("$text: ")

    return input.nextLine()
}

fun checkFile(f: File) {
    if (!f.exists()) {
        throw RuntimeException("File doesn't exist : $f")
    }else{
        println("Valid path: ${f.absolutePath}")
    }
}

class FileContentBuilder {
    val builder: StringBuilder = StringBuilder()

    fun appendLine(line: String) {
        builder.append(line).append("\n")
    }

    fun build(): String {
        return builder.toString()
    }
}

fun parse(doc: Document): Map<String, String> {
    val documentElement = doc.documentElement

    val map = HashMap<String, String>()

    val length = documentElement.childNodes.length
    for (i in 0 until length) {
        val item: Node = documentElement.childNodes.item(i)
        if (item.nodeName.startsWith("#")) continue
        val attributes = item.attributes
        val namedItem = attributes.getNamedItem("name")

        map[namedItem.textContent] = item.textContent
    }

    return map
}

fun readXml(xmlFile: File): Document {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
}