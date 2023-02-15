package moonrise.pjt3.debate.repository;

import io.lettuce.core.dynamic.annotation.Param;
import moonrise.pjt3.debate.entity.Message;
import moonrise.pjt3.member.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile,Long> {
    @Query(value = "select p from Profile p where nickname like :nickName")
    Profile findImagePathByNickName(@Param("nickName") String nickName);
}