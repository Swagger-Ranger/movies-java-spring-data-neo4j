package movies.spring.data.neo4j.movies;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Michael Hunger
 * @author Mark Angrish
 * @author Jennifer Reif
 * @author Michael J. Simons
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    private final MovieNeo4jRepository movieNeo4jRepository;

    private final Neo4jClient neo4jClient;

    private final Driver driver;

    private final DatabaseSelectionProvider databaseSelectionProvider;


    MovieService(MovieRepository movieRepository, MovieNeo4jRepository movieNeo4jRepository,
                 Neo4jClient neo4jClient,
                 Driver driver,
                 DatabaseSelectionProvider databaseSelectionProvider) {

        this.movieRepository = movieRepository;
        this.movieNeo4jRepository = movieNeo4jRepository;
        this.neo4jClient = neo4jClient;
        this.driver = driver;
        this.databaseSelectionProvider = databaseSelectionProvider;
    }

    // 更新方法
    @Transactional
    public void updateReleasedByTitle(String id, Integer released) {
        if (released == null) {
            throw new RuntimeException("Param released released is null");
        }
        Optional<Movie> movie = movieNeo4jRepository.findById(id);
        if (movie.isPresent()) {
            movie.get().setReleased(released); // 确保使用传入的ID更新
            movieNeo4jRepository.save(movie.get());
        } else {
            throw new RuntimeException("Movie with id " + id + " not found.");
        }
    }

    // 删除方法
    @Transactional
    public void deleteMovie(String title) {
        if (movieNeo4jRepository.existsById(title)) {
            movieNeo4jRepository.deleteById(title);
        } else {
            throw new RuntimeException("Movie with id " + title + " not found.");
        }
    }

    public Page<MovieResultDto> queryExamplePage(String title,
                                                 String sorter,
                                                 Integer pageSize,
                                                 Integer currentPage) {
        /*
         * 注意：因为语句里面要写成" MATCH (movie:Movie) WHERE movie.title CONTAINS "a" RETURN movie
         *     order by movie.released  desc skip 5 limit 10 " 。所以传入的sorter对象得是带 "movie." 前缀的，比如："movie.released"
         *     否则会报错org.neo4j.driver.exceptions.ClientException: Variable `released` not defined。
         *     更优雅的写法是自定义一个 CustomSortProcessor 排序处理器（感觉太麻烦，不实用），或者重新调整一下语句将排序的对象包含在返回里面。
         */
        String sortField = Optional.of(sorter).orElse("released");
        Sort sort = Sort.by("movie.".concat(sortField)).ascending();
        Page<Movie> movies = movieNeo4jRepository.queryExamplePage(title, PageRequest.of(currentPage, pageSize, sort));
        return movies.map(MovieResultDto::new);
    }


    public MovieDetailsDto fetchDetailsByTitle(String title) {
        return this.neo4jClient
                .query("""
                        MATCH (movie:Movie {title: $title})
                        OPTIONAL MATCH (person:Person)-[r]->(movie)
                        WITH movie, COLLECT({ name: person.name, job: REPLACE(TOLOWER(TYPE(r)), '_in', ''), role: HEAD(r.roles) }) as cast
                        RETURN movie { .title, cast: cast }
                        """)
                .in(database())
                .bindAll(Map.of("title", title))
                .fetchAs(MovieDetailsDto.class)
                .mappedBy(this::toMovieDetails)
                .one()
                .orElse(null);
    }

    public int voteInMovieByTitle(String title) {
        return this.neo4jClient
                .query("""
                        MATCH (m:Movie {title: $title})
                        WITH m, coalesce(m.votes, 0) AS currentVotes
                        SET m.votes = currentVotes + 1
                        """)
                .in(database())
                .bindAll(Map.of("title", title))
                .run()
                .counters()
                .propertiesSet();
    }

    public List<MovieResultDto> searchMoviesByTitle(String title) {
        return this.movieRepository.findSearchResults(title)
                .stream()
                .map(MovieResultDto::new)
                .toList();
    }

    /**
     * This is an example of when you might want to use the pure driver in case you have no need for mapping at all, neither in the
     * form of the way the {@link org.springframework.data.neo4j.core.Neo4jClient} allows and not in form of entities.
     *
     * @return A representation D3.js can handle
     */
    public Map<String, List<Object>> fetchMovieGraph() {

        var nodes = new ArrayList<>();
        var links = new ArrayList<>();

        try (Session session = sessionFor(database())) {
            var records = session.executeRead(tx -> tx.run("""
                    MATCH (m:Movie) <- [r:ACTED_IN] - (p:Person)
                    WITH m, p ORDER BY m.title, p.name
                    RETURN m.title AS movie, collect(p.name) AS actors
                    """
            ).list());
            records.forEach(record -> {
                var movie = Map.of("label", "movie", "title", record.get("movie").asString());

                var targetIndex = nodes.size();
                nodes.add(movie);

                record.get("actors").asList(Value::asString).forEach(name -> {
                    var actor = Map.of("label", "actor", "title", name);

                    int sourceIndex;
                    if (nodes.contains(actor)) {
                        sourceIndex = nodes.indexOf(actor);
                    } else {
                        nodes.add(actor);
                        sourceIndex = nodes.size() - 1;
                    }
                    links.add(Map.of("source", sourceIndex, "target", targetIndex));
                });
            });
        }
        return Map.of("nodes", nodes, "links", links);
    }

    private Session sessionFor(String database) {
        if (database == null) {
            return driver.session();
        }
        return driver.session(SessionConfig.forDatabase(database));
    }

    private String database() {
        return databaseSelectionProvider.getDatabaseSelection().getValue();
    }

    private MovieDetailsDto toMovieDetails(TypeSystem ignored, org.neo4j.driver.Record record) {
        var movie = record.get("movie");
        return new MovieDetailsDto(
                movie.get("title").asString(),
                movie.get("cast").asList((member) -> {
                    var result = new CastMemberDto(
                            member.get("name").asString(),
                            member.get("job").asString()
                    );
                    var role = member.get("role");
                    if (role.isNull()) {
                        return result;
                    }
                    return result.withRole(role.asString());
                })
        );
    }
}
