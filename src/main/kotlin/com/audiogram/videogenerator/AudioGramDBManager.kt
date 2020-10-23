package com.audiogram.videogenerator

import reactor.core.publisher.Mono
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

/*
* Manages task entries in the database.
* Does CRUD operations needed for logging and monitoring task progress
* */
sealed class AudioGramDBManager {

    companion object {

        private lateinit var conn: Connection
        private lateinit var statement: Statement
        private const val URL = "jdbc:sqlserver://35.223.32.43:1433;" + "database=TASKS;" + "user=sqlserver;" + "password=olubunmi;"
        private const val DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

        fun connect() {
            try {
                Class.forName(DRIVER)
                conn = DriverManager.getConnection(URL)
                statement = conn.createStatement()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun close() {
            try {
                conn.close()
                statement.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun addTask(id: String) {
            connect()
            try {
                statement.execute("INSERT INTO TASKS VALUES ('$id','QUEUED',0,CURRENT_TIMESTAMP)")
                println("New task added with id:$id")
                close()
            } catch (e: SQLException) {
                close()
                e.printStackTrace()
            }
            close()
        }

        fun removeTask(id: String) {
            connect()
            try {
                statement.execute("DELETE FROM TASKS WHERE ID='$id'")
                println("task removed with id:$id")
                close()
            } catch (e: SQLException) {
                e.printStackTrace()
                close()
            }
            close()
        }

        fun getProgress(id: String): Mono<Int> {
            connect()

            try {
                val result = statement.executeQuery("SELECT PROGRESS FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return Mono.just(result.getInt("PROGRESS").also { result.close() })//
                }
                close()
            } catch (e: SQLException) {
                e.printStackTrace()
                close()
            }
            close()
            return Mono.just(0)
        }

        fun getStatus(id: String): String {
            connect()
            try {
                val result = statement.executeQuery("SELECT STATUS FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return result.getString("STATUS").also { result.close() }
                }
            } catch (e: SQLException) {
                close()
                return "NONE"
            }
            close()
            return "NONE"
        }

        fun getTaskStartDate(id: String): String {
            connect()
            try {
                val result = statement.executeQuery("SELECT MODIFIED FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return result.getString("MODIFIED").also { result.close() }
                }
                close()
            } catch (e: SQLException) {
                close()
                e.printStackTrace()
            }
            close()
            return "NONE"
        }

        fun updateProgress(id: String, percentage: Int) {
            connect()
            try {
                statement.executeUpdate("UPDATE TASKS SET PROGRESS=$percentage WHERE ID='$id'")
                close()
            } catch (e: SQLException) {
                e.printStackTrace()
                close()
            }
            close()
        }

        fun updateStatus(id: String, status: String) {
            connect()
            try {
                statement.executeUpdate("UPDATE TASKS SET STATUS='$status' WHERE ID='$id'")
                close()
            } catch (e: SQLException) {
                e.printStackTrace()
                close()
            }
            close()
        }
    }

}
