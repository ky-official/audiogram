package com.audiogram.videogenerator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.modelmapper.internal.util.CopyOnWriteLinkedHashMap

sealed class AudioGramTaskManager {

    companion object {
        private var tasks = CopyOnWriteLinkedHashMap<String, AudioGramVideoTask>().also {
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(10)
                    for (task in it) {
                        task.value.render()
                        it.remove(task.key)
                    }
                }
            }
        }

        fun addTask(taskData: AudioGramData) {
            tasks[taskData.id] = AudioGramVideoTask(taskData)
        }

        fun cancelTask(id: String) {
            this.tasks.remove(id)
            AudioGramDBManager.updateStatus(id, "CANCELED")
        }

        fun taskIsRunning(id: String): Boolean {
            if (this.tasks[id] == null) {
                return false
            }
            return true
        }
    }
}