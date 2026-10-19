package it.matato.dietreminder.backend.profile

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation

import it.matato.dietreminder.common.model.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Profile", description = "Account profile")
@RestController
@RequestMapping("/api/user/profile")
class UserProfilingController(private val profiles: UserProfileService) {
    @GetMapping
    @Operation(summary = "Get your profile")
    fun getProfile(@AuthenticationPrincipal account: AccountDto): UserProfileDto = profiles.get(account.userId)

    @PostMapping
    @Operation(summary = "Update your profile")
    fun saveProfile(
        @AuthenticationPrincipal account: AccountDto,
        @RequestBody profile: ProfileUpdateRequest
    ): UserProfileDto =
        profiles.save(
            UserProfileDto(
                account.userId,
                profile.username,
                profile.dailyCalorieGoal ?: profiles.get(account.userId).dailyCalorieGoal
            )
        )
}
