package moonrise.pjt1.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import moonrise.pjt1.commons.response.ResponseDto;
import moonrise.pjt1.member.entity.Member;
import moonrise.pjt1.member.repository.MemberRepository;
import moonrise.pjt1.movie.entity.Movie;
import moonrise.pjt1.movie.repository.MovieRepository;
import moonrise.pjt1.party.dto.*;
import moonrise.pjt1.party.entity.*;
import moonrise.pjt1.party.repository.PartyCommentRepository;
import moonrise.pjt1.party.repository.PartyInfoRepository;
import moonrise.pjt1.party.repository.PartyJoinRepository;
import moonrise.pjt1.party.repository.PartyRepository;
import moonrise.pjt1.util.HttpUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
// token parsing 요청
// member_id 매핑
//DB
//responseDto 작성

@Service
@RequiredArgsConstructor
@Log4j2
public class PartyService {
    private final PartyRepository partyRepository;
    private final PartyCommentRepository partyCommentRepository;
    private final MovieRepository movieRepository;
    private final MemberRepository memberRepository;
    private final PartyJoinRepository partyJoinRepository;
    private final PartyInfoRepository partyInfoRepository;
    private final RedisTemplate redisTemplate;
    public ResponseDto readParty(String access_token, Long partyId) {
        Map<String,Object> result = new HashMap<>();
        ResponseDto responseDto = new ResponseDto();
        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        //***************redis 캐시서버**********************
        String key = "partyViewCnt::"+partyId;
        //캐시에 값이 없으면 레포지토리에서 조회 있으면 값을 증가시킨다.
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if(valueOperations.get(key)==null){
            valueOperations.set(
                    key,
                    String.valueOf(partyInfoRepository.findPartyViewCnt(partyId)+1),
                    20,
                    TimeUnit.MINUTES);
        }
        else{
            valueOperations.increment(key);
        }
        int viewCnt = Integer.parseInt((String) valueOperations.get(key));
        log.info("value:{}",viewCnt);
        //***************redis 캐시서버**********************
        //***************DB 조회**********************
        Optional<Party> findParty = partyRepository.findById(partyId);
        Party party = findParty.get();
        List<PartyComment> partyComments = partyCommentRepository.getCommentList(partyId);
        List<PartyJoin> partyJoins = party.getPartyJoins();
        if(findParty.isPresent()){
            PartyReadResponseDto partyReadResponseDto = new PartyReadResponseDto(party.getId(),party.getTitle(),party.getContent(),party.getPartyDate(),
                    party.getPartyPeople(),party.getLocation(),party.getPartyStatus(),
                    party.getMovie().getId(),partyJoins,partyComments,party.getDeadLine(), viewCnt);
            result.put("findParty",partyReadResponseDto);
        }
        if(user_id == party.getMember().getId()){
            result.put("isWriter",true);
        }
        else result.put("isWriter",false);
        //***************DB 조회**********************
        //responseDto 작성
        responseDto.setMessage("소모임 상세보기 리턴");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
    public ResponseDto listParty(Long movieId, PageRequest pageable) {
        Map<String,Object> result = new HashMap<>();
        ResponseDto responseDto = new ResponseDto();

        Page<Party> partyList = partyRepository.findPartyList(movieId, pageable);

        result.put("findParties",partyList.get());
        result.put("totalPages", partyList.getTotalPages());

        //responseDto 작성
        responseDto.setMessage("소모임 목록 리턴");
        responseDto.setData(result);
        responseDto.setStatus_code(200);

        return responseDto;
    }
    public ResponseDto createParty(String access_token, PartyCreateDto partyCreateDto) {
        Map<String, Object> result = new HashMap<>();

        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        ResponseDto responseDto = new ResponseDto();

        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        //DB
        Optional<Member> findMember = memberRepository.findById(user_id);
        Optional<Movie> findMovie = movieRepository.findById(partyCreateDto.getMovieId());
        PartyInfo partyInfo = new PartyInfo();
        Party party = Party.createParty(partyCreateDto, findMember.get(), findMovie.get(),partyInfo);
        partyRepository.save(party);

        //responseDto 작성
        result.put("party_id",party.getId());
        responseDto.setMessage("소모임 작성 완료");
        responseDto.setData(result);
        responseDto.setStatus_code(200);

        return responseDto;
    }
    @Transactional
    public ResponseDto modifyParty(String access_token,PartyModifyDto partyModifyDto) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        Party party = partyRepository.findById(partyModifyDto.getPartyId()).get();
        if(user_id == party.getMember().getId()) {
            party.modifyParty(partyModifyDto);
        }
        else{
            responseDto.setStatus_code(400);
            responseDto.setMessage("해당 소모임 주최자가 아닙니다.");
            return responseDto;
        }
        //responseDto 작성
        result.put("party_id",party.getId());
        responseDto.setMessage("소모임 수정 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
    @Transactional
    public ResponseDto createComment(String access_token, PartyCommentCreateDto partyCommentCreateDto) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        Long user_id = HttpUtil.requestParingToken(access_token);

        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        Optional<Member> findMember = memberRepository.findById(user_id);
        Optional<Party> findParty = partyRepository.findById(partyCommentCreateDto.getPartyId());

        PartyComment partyComment = PartyComment.createPartyComment(partyCommentCreateDto, findParty.get(), findMember.get());
        partyCommentRepository.save(partyComment);
        Long commentId = partyComment.getId();
        // 원댓글이면 groupId 를 본인 pk 로 저장
        if (partyComment.getGroupId() == 0L){
            partyComment.setGroupId(commentId);
        }
        //responseDto 작성
        result.put("commentId",commentId);
        responseDto.setMessage("댓글 작성 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }

    public ResponseDto createJoin(String access_token, PartyJoinCreateDto partyJoinCreateDto) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        Long user_id = HttpUtil.requestParingToken(access_token);

        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        Optional<Member> findMember = memberRepository.findById(user_id);
        Optional<Party> findParty = partyRepository.findById(partyJoinCreateDto.getPartyId());

        PartyJoin partyJoin = PartyJoin.createPartyJoin(partyJoinCreateDto,findMember.get(),findParty.get());
        partyJoinRepository.save(partyJoin);
        //responseDto 작성
        result.put("joinId",partyJoin.getId());
        responseDto.setMessage("참가신청 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
    @Transactional
    public ResponseDto updatePartyStatus(String access_token, Long partyId, int status) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        Long user_id = HttpUtil.requestParingToken(access_token);

        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        Party party = partyRepository.findById(partyId).get();
        if(user_id == party.getMember().getId()) {
            if (status == 1) {
                party.setPartyStatus(PartyStatus.모집완료);
            } else if (status == 2) {
                party.setPartyStatus(PartyStatus.기간만료);
            } else if (status == 3) {
                party.setPartyStatus(PartyStatus.삭제);
            }
        }
        else {
            responseDto.setStatus_code(400);
            responseDto.setMessage("해당 소모임 주최자가 아닙니다.");
            return responseDto;
        }
        //responseDto 작성
        result.put("partyStatus",party.getPartyStatus());
        responseDto.setMessage("소모임 상태 변경 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);

        return responseDto;
    }
    @Transactional
    public ResponseDto updateJoinStatus(String access_token, Long joinId, int status) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        Long user_id = HttpUtil.requestParingToken(access_token);
        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        PartyJoin partyJoin = partyJoinRepository.findById(joinId).get();
        if(user_id == partyJoin.getParty().getMember().getId()) {
            if (status == 1) {
                partyJoin.setPartyJoinStatus(PartyJoinStatus.승인);
            } else if (status == 2) {
                partyJoin.setPartyJoinStatus(PartyJoinStatus.거절);
            } else if (status == 3) {
                partyJoin.setPartyJoinStatus(PartyJoinStatus.취소);
            }
        }
        else {
            responseDto.setStatus_code(400);
            responseDto.setMessage("해당 소모임 주최자가 아닙니다.");
            return responseDto;
        }
        //responseDto 작성
        result.put("partyJoinStatus",partyJoin.getPartyJoinStatus());
        responseDto.setMessage("참가 신청 상태 변경 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
    @Transactional
    public ResponseDto updateComment(String access_token, PartyCommentUpdateDto partyCommentUpdateDto) {
        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }

        Long commentId = partyCommentUpdateDto.getCommentId();
        Optional<PartyComment> findComment = partyCommentRepository.findById(commentId);
        if(!findComment.isPresent()){
            responseDto.setStatus_code(400);
            responseDto.setMessage("수정할 댓글을 찾을 수 없습니다.");
            return responseDto;
        }
        PartyComment partyComment = findComment.get();
        if(user_id == partyComment.getMember().getId()){
            partyComment.setContent(partyCommentUpdateDto.getContent());
            partyComment.setShowPublic(partyCommentUpdateDto.isShowPublic());
            partyComment.setCommentWriteTime(LocalDateTime.now());
        }
        else{
            responseDto.setStatus_code(400);
            responseDto.setMessage("해당 댓글 작성자가 아닙니다.");
            return responseDto;
        }
        //responseDto 작성
        result.put("partyCommentId", commentId);
        responseDto.setMessage("댓글 수정 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
    @Transactional
    public ResponseDto statusComment(String access_token, Long commentId, int statusCode) {

        ResponseDto responseDto = new ResponseDto();
        Map<String, Object> result = new HashMap<>();
        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        Optional<PartyComment> findComment = partyCommentRepository.findById(commentId);
        if(!findComment.isPresent()){
            responseDto.setStatus_code(400);
            responseDto.setMessage("수정할 댓글을 찾을 수 없습니다.");
            return responseDto;
        }
        PartyComment partyComment = findComment.get();
        if(user_id == partyComment.getMember().getId()) {
            switch (statusCode) {
                case 1:
                    partyComment.normalize();
                    break;
                case 2:
                    partyComment.banned();
                    break;
                case 3:
                    partyComment.deleted();
                    break;
            }
        }
        else{
            responseDto.setStatus_code(400);
            responseDto.setMessage("해당 댓글 작성자가 아닙니다.");
            return responseDto;
        }
        //responseDto 작성
        result.put("partyCommentStatus", partyComment.getPartyCommentStatus());
        responseDto.setMessage("댓글 상태 변경 성공");
        responseDto.setData(result);
        responseDto.setStatus_code(200);
        return responseDto;
    }
}