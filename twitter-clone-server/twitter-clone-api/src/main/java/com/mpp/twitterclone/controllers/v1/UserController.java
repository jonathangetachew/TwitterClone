package com.mpp.twitterclone.controllers.v1;

import com.mpp.twitterclone.controllers.v1.resourceassemblers.UserResourceAssembler;
import com.mpp.twitterclone.model.User;
import com.mpp.twitterclone.services.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by Jonathan on 9/8/2019.
 */

@RestController
@RequestMapping(value = UserController.BASE_URL, produces = MediaTypes.HAL_JSON_VALUE,
				consumes = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

	public static final String BASE_URL = "/api/v1/users";

	private final UserService userService;

	private final UserResourceAssembler userResourceAssembler;

	public UserController(UserService userService, UserResourceAssembler userResourceAssembler) {
		this.userService = userService;
		this.userResourceAssembler = userResourceAssembler;
	}

	///> Get Mappings
	@ApiOperation(value = "Get all Users",
			notes = "This operation can only be done by an ADMIN.")
	@GetMapping
	public Resources<Resource<User>> getAllUsers() {
		List<Resource<User>> users = userService.findAll().stream()
				.map(userResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(users,
				linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());
	}

	@ApiOperation(value = "Get a User by Username")
	@GetMapping("/{username}")
	public Resource<User> getUserByUsername(@PathVariable String username) {
		return userResourceAssembler.toResource(userService.findUserByUsername(username));
	}

	@ApiOperation(value = "Get Follower List by User ID")
	@GetMapping("/{userId}/followers")
	public Resources<Resource<User>> getAllFollowersById(@PathVariable String userId) {
		List<Resource<User>> users = userService.findAllFollowers(userId).stream()
				.map(userResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(users,
				linkTo(methodOn(UserController.class).getAllFollowersById(userId)).withSelfRel());
	}

	@ApiOperation(value = "Get Following List by User ID")
	@GetMapping("/{userId}/following")
	public Resources<Resource<User>> getAllFollowingById(@PathVariable String userId) {
		List<Resource<User>> users = userService.findAllFollowing(userId).stream()
				.map(userResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(users,
				linkTo(methodOn(UserController.class).getAllFollowingById(userId)).withSelfRel());
	}

	///> Post Mappings
	@ApiOperation(value = "Create a User",
					notes = "Password won't be encrypted. Use signup action instead.")
	@PostMapping("/create")
	public ResponseEntity<Resource<User>> createUser(@RequestBody User user) throws URISyntaxException {
		Resource<User> userResource = userResourceAssembler.toResource(userService.create(user));

		return ResponseEntity
				.created(new URI(userResource.getId().expand().getHref()))
				.body(userResource);
	}

	@ApiOperation(value = "Follow a User",
					notes = "This operation can only be done by an authenticated user.")
	@PostMapping("/{id}/follow")
	public ResponseEntity<Resource<User>> followUser(@PathVariable String id, Principal principal) throws URISyntaxException {
		Resource<User> userResource = userResourceAssembler.toResource(userService.followUser(id,
																							principal.getName()));

		return ResponseEntity
				.created(new URI(userResource.getId().expand().getHref()))
				.body(userResource);
	}

	///> Put Mappings
	@ApiOperation(value = "Update a User",
					notes = "This operation can only be done by the owner.")
	@PutMapping("/{id}/update")
	public ResponseEntity<Resource<User>> updateUser(@RequestBody User user,
	                                                 @PathVariable String id,
	                                                 Principal principal) throws URISyntaxException {
		Resource<User> userResource = userResourceAssembler.toResource(userService.update(user, id, principal.getName()));

		return ResponseEntity
				.created(new URI(userResource.getId().expand().getHref()))
				.body(userResource);
	}

	///> Delete Mappings
	@ApiOperation(value = "Delete User by ID",
					notes = "This operation can only be done by the owner.")
	@DeleteMapping("/{id}/remove")
	public ResponseEntity<?> deleteUser(@PathVariable String id, Principal principal) {
		userService.deleteById(id, principal.getName());

		Map<String, String> responseMessage = new HashMap<>();
		responseMessage.put("message", "User Removed Successfully");

		return ResponseEntity
				.accepted()
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseMessage);
	}
}
