package com.mpp.twitterclone.controllers.v1;

import com.mpp.twitterclone.controllers.v1.resourceassemblers.TweetResourceAssembler;
import com.mpp.twitterclone.model.Tweet;
import com.mpp.twitterclone.services.TweetService;
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
@RequestMapping(value = TweetController.BASE_URL, produces = MediaTypes.HAL_JSON_VALUE,
				consumes = MediaType.APPLICATION_JSON_VALUE)
public class TweetController {

	public static final String BASE_URL = "/api/v1/tweets";

	private final TweetService tweetService;

	private final TweetResourceAssembler tweetResourceAssembler;

	public TweetController(TweetService tweetService, TweetResourceAssembler tweetResourceAssembler) {
		this.tweetService = tweetService;
		this.tweetResourceAssembler = tweetResourceAssembler;
	}

	///> Get Mappings
	@ApiOperation(value = "Get all Tweets",
					notes = "This operation can only be done by an ADMIN.")
	@GetMapping
	public Resources<Resource<Tweet>> getAllTweets() {
		List<Resource<Tweet>> tweets = tweetService.findAll().stream()
				.map(tweetResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(tweets,
				linkTo(methodOn(TweetController.class).getAllTweets()).withSelfRel());
	}

	@ApiOperation(value = "Get Tweets by Username",
					notes = "It can be done by any user.")
	@GetMapping("/user/{username}")
	public Resources<Resource<Tweet>> getAllTweetsByUsername(@PathVariable String username) {
		List<Resource<Tweet>> tweets = tweetService.findAllTweetsByUsername(username).stream()
				.map(tweetResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(tweets,
				linkTo(methodOn(TweetController.class).getAllTweetsByUsername(username)).withSelfRel());
	}

	@ApiOperation(value = "Get Tweet by ID")
	@GetMapping("/{id}")
	public Resource<Tweet> getTweetById(@PathVariable String id) {
		return tweetResourceAssembler.toResource(tweetService.findById(id));
	}

	@ApiOperation(value = "This will get a list of all replies for a given tweet.")
	@GetMapping("/{id}/replies")
	public Resources<Resource<Tweet>> getTweetReplies(@PathVariable String id) {
		List<Resource<Tweet>> tweets = tweetService.findAllReplies(id).stream()
				.map(tweetResourceAssembler::toResource)
				.collect(Collectors.toList());

		return new Resources<>(tweets,
				linkTo(methodOn(TweetController.class).getTweetReplies(id)).withSelfRel());
	}

	// todo: getFavoriteTweets

	///> Post Mappings
	@ApiOperation(value = "Create a Tweet",
					notes = "This operation can only be done by an authenticated user.")
	@PostMapping("/create")
	public ResponseEntity<Resource<Tweet>> createTweet(@RequestBody Tweet tweet) throws URISyntaxException {
		Resource<Tweet> tweetResource = tweetResourceAssembler.toResource(tweetService.create(tweet));

		return ResponseEntity
				.created(new URI(tweetResource.getId().expand().getHref()))
				.body(tweetResource);
	}

	@ApiOperation(value = "Reply/Comment to/on a Tweet",
					notes = "This operation can only be done by an authenticated user.")
	@PostMapping("/{originalTweetId}/reply")
	public ResponseEntity<Resource<Tweet>> reply(@RequestBody Tweet replyTweet,
	                                             @PathVariable String originalTweetId) throws URISyntaxException {
		Resource<Tweet> tweetResource = tweetResourceAssembler.toResource(tweetService.replyToTweet(replyTweet, originalTweetId));

		return ResponseEntity
				.created(new URI(tweetResource.getId().expand().getHref()))
				.body(tweetResource);
	}

//	@PostMapping("/{id}/retweet")
//	public ResponseEntity<Resource<Tweet>> retweet(@PathVariable String id) throws URISyntaxException {
//		Resource<Tweet> tweetResource = tweetResourceAssembler.toResource(tweetService.retweetTweet(id, "test"));
//
//		return ResponseEntity
//				.created(new URI(tweetResource.getId().expand().getHref()))
//				.body(tweetResource);
//	}

	@ApiOperation(value = "Favorite a Tweet",
					notes = "This operation can only be done by an authenticated user.")
	@PostMapping("/{id}/favorite")
	public ResponseEntity<Resource<Tweet>> favorite(@PathVariable String id) throws URISyntaxException {
		// Todo: change user id to id from principal object
		Resource<Tweet> tweetResource = tweetResourceAssembler.toResource(tweetService.favoriteTweet(id, "test"));

		return ResponseEntity
				.created(new URI(tweetResource.getId().expand().getHref()))
				.body(tweetResource);
	}

	///> Put Mappings
	@ApiOperation(value = "Update a Tweet",
			notes = "This operation can only be done by an authenticated user.")
	@PutMapping("/{originalTweetId}/update")
	public ResponseEntity<Resource<Tweet>> updateTweet(@RequestBody Tweet newTweet,
	                                                   @PathVariable String originalTweetId,
	                                                   Principal principal) throws URISyntaxException {
		Resource<Tweet> tweetResource = tweetResourceAssembler.toResource(tweetService.update(newTweet, originalTweetId,
																								principal.getName()));

		return ResponseEntity
				.created(new URI(tweetResource.getId().expand().getHref()))
				.body(tweetResource);
	}

	///> Delete Mappings
	@ApiOperation(value = "Delete Tweet by ID",
			notes = "This operation can only be done by the owner of the tweet.")
	@DeleteMapping("/{tweetId}/remove")
	public ResponseEntity<?> deleteTweet(@PathVariable String tweetId, Principal principal) {
		tweetService.deleteById(tweetId, principal.getName());

		Map<String, String> responseMessage = new HashMap<>();
		responseMessage.put("message", "Tweet Removed Successfully");

		return ResponseEntity
				.accepted()
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseMessage);
	}
}
