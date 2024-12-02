package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.File

private lateinit var ALL_FILES: List<FileHandle>

fun loadFiles() {
    val files = Gdx.files.internal("assets.txt").readString().split("\n")
    ALL_FILES = files.map { Gdx.files.internal(it) }
}

fun listFolder(s: String): List<FileHandle> {
    return ALL_FILES.filter { it.path().startsWith(s) }.map {
        var path = it
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            val pathStr = System.getProperty("user.dir") + File.separator + "assets" + File.separator + "$path"
            path = Gdx.files.absolute(pathStr)
        }
        path
    }
}