package movies.spring.data.neo4j.movies;

import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * Neo4jRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T>,CrudRepository<T, ID>
 * Neo4jRepository 继承了 PagingAndSortingRepository(PagingAndSortingRepository<T, ID> extends Repository<T, ID>)
 * 来提供基本的 CRUD 操作，并在此基础上增加了分页和排序功能
 * 并又通过 继承 QueryByExampleExecutor 来实现给定的示例实体对象来执行查询，而不需要编写复杂的查询语句。
 * spring data的一些命名约定 https://springdoc.cn/spring-data-jpa/#repositories.namespace-reference
 *
 * @author liufei
 */
interface MovieNeo4jRepository extends Neo4jRepository<Movie, Long> {

}
