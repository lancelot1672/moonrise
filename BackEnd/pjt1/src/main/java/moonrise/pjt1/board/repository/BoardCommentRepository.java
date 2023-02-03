package moonrise.pjt1.board.repository;

import io.lettuce.core.dynamic.annotation.Param;
import moonrise.pjt1.board.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment,Long> {
    @Query(value = "select bc from BoardComment as bc where bc.board.id =:boardId order by bc.groupId ")
    List<BoardComment> getCommentList(@Param("boardId") Long boardId);

}
