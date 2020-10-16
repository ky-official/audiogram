package com.audiogram.app.controllers

import com.audiogram.videogenerator.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeType
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class MainController {

    @Autowired
    private val request: HttpServletRequest? = null


    @GetMapping("/video/{id}")
    @ResponseBody
    fun getVideoController(@PathVariable id: String, response: HttpServletResponse): ResponseEntity<FileSystemResource> {
        var vid = AudioGramFileManager.getExport(id)
        var length = vid.length()

        val headers = HttpHeaders()
        headers.contentType = MediaType.asMediaType(MimeType.valueOf("video/mp4"))
        headers.contentLength = length
        headers.setContentDispositionFormData("attachment", "video_$id.mp4")

        return ResponseEntity(FileSystemResource(vid), headers, HttpStatus.OK)

    }

    @GetMapping("/progress/{id}")
    @CrossOrigin
    fun progressController(@PathVariable id: String): Int {//Mono<Int>
        return 30//AudioGramDBManager.getProgress(id)
    }

    @GetMapping("/status/{id}")
    @CrossOrigin
    fun statusController(@PathVariable id: String): String {
        return ""//AudioGramDBManager.getStatus(id)
    }

    @GetMapping("/")
    @CrossOrigin
    fun testController(): String {
        return "somethhing"
    }

    @GetMapping("/cancel/{id}")
    @CrossOrigin
    fun cancelController(@PathVariable id: String): String {

        CoroutineScope(Dispatchers.IO).launch {
            AudioGramTaskManager.cancelTask(id)
        }
        return "canceled task with id:$id"
    }

    @DeleteMapping("/delete/{id}")
    @CrossOrigin
    fun deleteController(@PathVariable id: String): ResponseEntity<Any> {

        when {
            AudioGramDBManager.getStatus(id) in arrayOf("RUNNING", "QUEUED") -> CoroutineScope(Dispatchers.IO).launch {
                AudioGramTaskManager.cancelTask(id)
                AudioGramDBManager.removeTask(id)
                AudioGramFileManager.deleteTaskDirectory(id)
            }
            AudioGramDBManager.getStatus(id) in arrayOf("CANCELED", "FINISHED") -> {
                AudioGramDBManager.removeTask(id)
                AudioGramFileManager.deleteTaskDirectory(id)
            }
            else -> {
                return ResponseEntity(id, HttpStatus.NOT_FOUND)
            }
        }
        return ResponseEntity(id, HttpStatus.OK)
    }


    @PostMapping("/generate")
    @CrossOrigin
    fun postController() {
        try {
            var data = AudioGramData()
            data.initialize(request!!.parts)
            AudioGramTaskManager.addTask(data)
            println("controller returned")
        } catch (e: AudioGramException) {
            println(e.message)
        }

    }
}
//-Xmx512m -XX:MaxDirectMemorySize=512m
