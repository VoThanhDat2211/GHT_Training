package com.multimodule.user.adapter.in.web;

import com.multimodule.user.adapter.in.web.dto.request.CreateUserRequest;
import com.multimodule.user.adapter.in.web.dto.response.UserRestResponse;
import com.multimodule.user.adapter.in.web.mapper.UserWebMapper;
import com.multimodule.user.application.port.input.command.CreateUserUseCase;
import com.multimodule.user.application.port.input.command.DeleteUserUseCase;
import com.multimodule.user.application.port.input.query.GetUserByIdQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final GetUserByIdQuery getUserByIdQuery;
    private final UserWebMapper userWebMapper;

    @PostMapping
    public ResponseEntity<UserRestResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserRestResponse response = userWebMapper.toRestResponse(
                createUserUseCase.createUser(userWebMapper.toCommand(request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRestResponse> getUserById(@PathVariable UUID id) {
        UserRestResponse response = userWebMapper.toRestResponse(
                getUserByIdQuery.getUserById(id)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        deleteUserUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
