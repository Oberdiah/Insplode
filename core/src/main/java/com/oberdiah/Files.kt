package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

private lateinit var ALL_FILES: List<FileHandle>

fun loadFiles() {
    val files = Gdx.files.internal("assets.txt").readString().split("\n")
    ALL_FILES = files.map { Gdx.files.internal(it) }
}

fun listFolder(s: String): List<FileHandle> {
    return ALL_FILES.filter { it.path().startsWith(s) }
}