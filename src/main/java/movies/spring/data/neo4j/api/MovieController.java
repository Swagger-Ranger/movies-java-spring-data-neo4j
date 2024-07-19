package movies.spring.data.neo4j.api;

import movies.spring.data.neo4j.movies.MovieDetailsDto;
import movies.spring.data.neo4j.movies.MovieResultDto;
import movies.spring.data.neo4j.movies.MovieService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author Michael J. Simons
 */
@RestController
class MovieController {

	private final MovieService movieService;

	MovieController(MovieService movieService) {
		this.movieService = movieService;
	}

	@GetMapping("/movie/{title}")
	public MovieDetailsDto findByTitle(@PathVariable("title") String title) {
		return movieService.fetchDetailsByTitle(title);
	}

	@PostMapping("/movie/{title}/vote")
	public int voteByTitle(@PathVariable("title") String title) {
		return movieService.voteInMovieByTitle(title);
	}

	@GetMapping("/movie/released")
	public void updateReleasedByTitle(@RequestParam("title") String title, @RequestParam("released") Integer released) {
		movieService.updateReleasedByTitle(title, released);
	}

	@GetMapping("/movie/examplePage")
	public Page<MovieResultDto> queryExamplePage(@RequestParam("title") String title,
												 @RequestParam("sorter") String sorter,
												 @RequestParam("pageSize") Integer pageSize,
												 @RequestParam("currentPage") Integer currentPage) {
		return movieService.queryExamplePage(title, sorter, pageSize, currentPage);
	}

	@DeleteMapping("/movie/{title}")
	public void deleteByTitle(@PathVariable("title") String title) {
		movieService.deleteMovie(title);
	}

	@GetMapping("/search")
	List<MovieResultDto> search(@RequestParam("q") String title) {
		return movieService.searchMoviesByTitle(stripWildcards(title));
	}

	@GetMapping("/graph")
	public Map<String, List<Object>> getGraph() {
		return movieService.fetchMovieGraph();
	}

	private static String stripWildcards(String title) {
		String result = title;
		if (result.startsWith("*")) {
			result = result.substring(1);
		}
		if (result.endsWith("*")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}
}
