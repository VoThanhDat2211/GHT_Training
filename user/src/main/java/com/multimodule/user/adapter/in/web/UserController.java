package com.multimodule.user.adapter.in.web;

import com.multimodule.user.adapter.in.web.dto.request.CreateUserRequest;
import com.multimodule.user.adapter.in.web.dto.request.UpdateUserRequest;
import com.multimodule.user.adapter.in.web.dto.response.UserRestResponse;
import com.multimodule.user.adapter.in.web.mapper.UserWebMapper;
import com.multimodule.user.application.port.input.command.ActivateUserUseCase;
import com.multimodule.user.application.port.input.command.BlockUserUseCase;
import com.multimodule.user.application.port.input.command.CreateUserUseCase;
import com.multimodule.user.application.port.input.command.DeleteUserUseCase;
import com.multimodule.user.application.port.input.command.UpdateUserUseCase;
import com.multimodule.user.application.port.input.query.GetUserByIdQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final BlockUserUseCase blockUserUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final GetUserByIdQuery getUserByIdQuery;
    private final UserWebMapper userWebMapper;

    @PostMapping
    public ResponseEntity<UserRestResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserRestResponse response = userWebMapper.toRestResponse(
                createUserUseCase.createUser(userWebMapper.toCreateCommand(request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRestResponse> getUserById(@PathVariable("id") UUID id) {
        UserRestResponse response = userWebMapper.toRestResponse(getUserByIdQuery.getUserById(id));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserRestResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserRestResponse response = userWebMapper.toRestResponse(
                updateUserUseCase.updateUser(id, userWebMapper.toUpdateCommand(request))
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<UserRestResponse> blockUser(@PathVariable UUID id) {
        UserRestResponse response = userWebMapper.toRestResponse(blockUserUseCase.blockUser(id));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserRestResponse> activateUser(@PathVariable UUID id) {
        UserRestResponse response = userWebMapper.toRestResponse(activateUserUseCase.activateUser(id));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        deleteUserUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
