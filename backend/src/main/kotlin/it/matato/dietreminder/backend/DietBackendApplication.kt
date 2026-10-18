package it.matato.dietreminder.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration::class])
class DietBackendApplication

fun main(args: Array<String>) {
    runApplication<DietBackendApplication>(*args)
}
