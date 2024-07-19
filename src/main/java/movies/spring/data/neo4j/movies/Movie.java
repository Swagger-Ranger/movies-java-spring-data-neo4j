package movies.spring.data.neo4j.movies;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;

/**
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@Node
public class Movie {

	/*
	 * @Node 注解表明这是一个 Neo4j 节点的实体类
	 * @Id 是实体类的必需注解，表明被标记的属性是唯一标识符，也就是主键；
	 */
	@Id
	private final Long id;

	private final String title;

	private final String tagline;

	private Integer released;

	private Long votes;

	public Movie(Long id, String title, String tagline) {
        this.id = id;
        this.title = title;
		this.tagline = tagline;
	}

	public String getTitle() {
		return title;
	}

	public String getTagline() {
		return tagline;
	}

	public Integer getReleased() {
		return released;
	}

	public Long getVotes() {
		return votes;
	}

	public void setReleased(Integer released) {
		this.released = released;
	}

	public void setVotes(Long votes) {
		this.votes = votes;
	}
}
