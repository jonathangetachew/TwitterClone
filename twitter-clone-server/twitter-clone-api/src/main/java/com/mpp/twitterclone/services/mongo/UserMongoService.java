package com.mpp.twitterclone.services.mongo;

import com.mpp.twitterclone.exceptions.ResourceExistsException;
import com.mpp.twitterclone.exceptions.ResourceNotFoundException;
import com.mpp.twitterclone.model.Follow;
import com.mpp.twitterclone.model.Role;
import com.mpp.twitterclone.model.User;
import com.mpp.twitterclone.repositories.FollowRepository;
import com.mpp.twitterclone.repositories.UserRepository;
import com.mpp.twitterclone.services.RoleService;
import com.mpp.twitterclone.services.UserService;
import com.mpp.twitterclone.validators.UserActionValidator;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Jonathan on 9/8/2019.
 */

@Service
public class UserMongoService implements UserService {

	private final UserRepository userRepository;

	private final FollowRepository followRepository;

	private final RoleService roleService;

	private final PasswordEncoder passwordEncoder;

	private final UserActionValidator userActionValidator;

	public UserMongoService(UserRepository userRepository, FollowRepository followRepository,
	                        RoleService roleService, PasswordEncoder passwordEncoder,
	                        @Lazy UserActionValidator userActionValidator) {
		this.userRepository = userRepository;
		this.roleService = roleService;
		this.followRepository = followRepository;
		this.passwordEncoder = passwordEncoder;
		this.userActionValidator = userActionValidator;
	}

	@Override
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@Override
	public List<User> findAllFollowers(String followedUserId) {
		List<Follow> follows = followRepository.findAllByFollowedUserId(followedUserId);

		return follows.stream()
				.map(follow -> findById(follow.getFollowerUserId())).collect(Collectors.toList());
	}

	@Override
	public List<User> findAllFollowing(String followerUserId) {
		List<Follow> follows = followRepository.findAllByFollowerUserId(followerUserId);

		return follows.stream()
				.map(follow -> findById(follow.getFollowerUserId())).collect(Collectors.toList());
	}

	@Override
	public User findById(String id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User"));
	}

	@Override
	public User findUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User"));
	}

	@Override
	public User create(User newUser) {

		// Todo:

		User user = userRepository.findByUsername(newUser.getUsername()).orElse(null);

		if ( user == null ) {
			// Encrypt User's Password
			newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

			// Assign Roles as Objects because they're sent as an array of strings in the request
			Set<Role> userRoles = new HashSet<>();
			if (newUser.getRoles() != null) {
				newUser.getRoles().forEach(role -> {
					Role role1 = roleService.findRoleByName(role.getName());
					if ( role1 == null ) throw new ResourceNotFoundException("Role");
					userRoles.add(role1);
				});
			}

			newUser.setRoles(userRoles);

			return userRepository.insert(newUser);
		}
		else throw new ResourceExistsException("User");
	}

	@Override
	public User followUser(String followedUserId, String followerUserId) {
		// Original Tweet
		User followedUser = findById(followedUserId);

		// Check for retweet record
		Optional<Follow> follow = followRepository.findByFollowerUserIdAndFollowedUserId(followerUserId, followedUserId);

		Integer currentFollowerCount = followedUser.getFollowersCount();

		if (follow.isPresent()) { // User performing the unfollow action

			// Remove record of current user favorite the tweet
			followRepository.deleteByFollowerUserIdAndAndFollowedUserId(follow.get().getFollowerUserId(),
																		follow.get().getFollowedUserId());

			// Update tweet data
			followedUser.setFollowersCount(--currentFollowerCount);

		} else { // User performing the favorite action
			// Keep a record of current user favorite the tweet
			followRepository.insert(Follow.builder().followedUserId(followedUserId)
													.followerUserId(followerUserId).build());

			// Update tweet data
			followedUser.setFollowersCount(++currentFollowerCount);
		}

		return update(followedUser, followedUserId, followerUserId);
	}

	@Override
	public User update(User newUser, String id, String currentUsername) {
		return userRepository.findById(id)
				.map(u -> {

					// Check if user performing the update is the owner
					userActionValidator.validateUserAction(currentUsername, newUser.getUsername());

					User user = userRepository.findByUsername(newUser.getUsername()).orElse(null);

					// Check if the the new username/email isn't already taken
					if (user != null && !user.getId().equals(id)) {
						if (user.getUsername() == newUser.getUsername()) throw new ResourceExistsException("Username");
						if (user.getEmail() == newUser.getEmail()) throw new ResourceExistsException("Email");
					}

					u.setUsername(newUser.getUsername());
					u.setPassword(newUser.getPassword());
					u.setName(newUser.getName());
					u.setEmail(newUser.getEmail());
					u.setGender(newUser.getGender());
					u.setDateOfBirth(newUser.getDateOfBirth());
					u.setProfileBannerUrl(newUser.getProfileBannerUrl());
					u.setProfileImageUrl(newUser.getProfileImageUrl());
					u.setPhoneNumber(newUser.getPhoneNumber());
					u.setUrl(newUser.getUrl());
					u.setDescription(newUser.getDescription());
					u.setProtect(newUser.getProtect());
					u.setVerified(newUser.getVerified());
					u.setFollowersCount(newUser.getFollowersCount());
					u.setFriendsCount(newUser.getFriendsCount());
					u.setRoles(newUser.getRoles());

					return userRepository.save(u);

				})
				.orElseThrow(() -> new ResourceNotFoundException("User"));
	}

	@Override
	public void delete(User user, String currentUsername) {
		/**
		 * Not accessible for controller endpoint
		 */
		// Check if the Tweet exists in db
		findById(user.getId());

		userRepository.delete(user);
	}

	@Override
	public void deleteById(String id, String currentUsername) {
		// Check if the Tweet exists in db
		User user = findById(id);

		// Check if user performing the update is the owner
		userActionValidator.validateUserAction(currentUsername, user.getUsername());

		userRepository.deleteById(id);
	}
}
