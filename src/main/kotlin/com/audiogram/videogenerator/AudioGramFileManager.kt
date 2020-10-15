package com.audiogram.videogenerator

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.Part

/*
* Creates the directories for a task.
* Stores resources to a task directory and returns the resource url.
* Retrieves resource files from a task directory.
* Deletes a task directory
* It is does not store the url of the task directory (it has no state).*/
sealed class AudioGramFileManager {

    companion object {
        private const val ROOT = "appData"

        fun createTaskDirectory(id: String) {
            val taskDirectory = File("$ROOT/tasks/task_$id")
            println(taskDirectory.absolutePath)

            if (taskDirectory.mkdir()) {
                println("New task directory created with id:$id")
                val resourceDirectory = File("$ROOT/tasks/task_$id/resources")
                val exportDirectory = File("$ROOT/tasks/task_$id/export")

                if (resourceDirectory.mkdir()) {
                    val audioDirectory = File("$ROOT/tasks/task_$id/resources/audio")
                    val imagesDirectory = File("$ROOT/tasks/task_$id/resources/images")
                    val videoDirectory = File("$ROOT/tasks/task_$id/resources/video")

                    audioDirectory.mkdir()
                    imagesDirectory.mkdir()
                    videoDirectory.mkdir()
                    println("New resource directory created in task directory with id:$id")

                } else {
                    println("Couldn't create resource directory in task directory with id:$id")
                }
                if (exportDirectory.mkdir()) {
                    println("New export directory created in task directory with id:$id")
                } else {
                    println("Couldn't create resource directory in task directory with id:$id")
                }
            } else {
                println("Couldn't create task directory with id:$id")
                throw AudioGramException("task exists")
            }
        }

        fun deleteTaskDirectory(id: String) {

            try {
                val folder = File("$ROOT/tasks/task_$id")
                FileUtils.deleteDirectory(folder)
                println("Deleted task directory with id:$id")
            } catch (e: Exception) {
                println("folder does not exist")
                e.printStackTrace()

            }
        }

        fun createVideoContainer(id: String): String {
            var path: String = "$ROOT/tasks/task_$id/export/video.mp4"
            var videoContainer = File(path)
            videoContainer.createNewFile()
            return path
        }

        fun getResource(path: String?): File {
            return File(path)
        }

        fun getExportBytes(id: String): InputStream {
            return Files.newInputStream(Paths.get("$ROOT/tasks/task_$id/export/video.mp4"))
        }

        fun getExport(id: String): File {
            return File("$ROOT/tasks/task_$id/export/video.mp4")
        }

        fun saveResource(type: String, id: String, resource: Part): String? {

            var filePath: Path
            try {
                when (type) {
                    "audio" -> {
                        var path: String = "$ROOT/tasks/task_$id/resources/audio"
                        filePath = Paths.get(path, "audio.${resource.submittedFileName.substringAfterLast(".")}")
                        try {
                            var os = Files.newOutputStream(filePath)
                            os.write(resource.inputStream.readBytes())
                            os.close()
                            return filePath.toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "image" -> {
                        var path: String = "$ROOT/tasks/task_$id/resources/images"
                        filePath = Paths.get(path, "${resource.name}.${resource.contentType.substringAfter("/")}")
                        try {
                            var os = Files.newOutputStream(filePath)
                            os.write(resource.inputStream.readBytes())
                            os.close()
                            return filePath.toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "video" -> {
                        var path: String = "$ROOT/tasks/task_$id/resources/video"
                        filePath = Paths.get(path, "video.${resource.submittedFileName.substringAfterLast(".")}")
                        try {
                            var os = Files.newOutputStream(filePath)
                            os.write(resource.inputStream.readBytes())
                            os.close()
                            return filePath.toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        println("invalid resource type passed: $type")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}