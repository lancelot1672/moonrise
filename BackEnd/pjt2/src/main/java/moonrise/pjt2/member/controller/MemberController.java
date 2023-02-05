package moonrise.pjt2.member.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moonrise.pjt2.member.exception.UnauthorizedException;
import moonrise.pjt2.member.model.entity.Member;
import moonrise.pjt2.member.model.service.MemberService;

import moonrise.pjt2.util.HttpUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.HashMap;

@RestController
@RequestMapping("/member")
@Slf4j
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    /**
     *
     * KaKao 서버로 부터 인가 코드를 받아
     * Access-Token 과 Refresh-Token을 받는다.
     */
    @PostMapping("/kakao")
    @Transactional
    public ResponseEntity<?> getKaKaoToken(@RequestHeader HttpHeaders headers){
        // Http Header 에서 인가 코드 받기
        String authorization_code = headers.get("authorization_code").toString();

        log.info("code : {}", authorization_code);

        String access_Token = "";
        String refresh_Token = "";

        HashMap<String, Object> resultMap = new HashMap<>();
        ResponseDto responseDto = new ResponseDto();
        String requestURL = "https://kauth.kakao.com/oauth/token";

        try{
            URL url = new URL(requestURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //POST 요청을 위해 기본값이 false인 setDoOutput을 true로
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            // POST 요청에 필요로 요고하는 파라미터 스트림을 통해 전송
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            StringBuilder sb = new StringBuilder();

            sb.append("grant_type=authorization_code");
            sb.append("&client_id=f630ff6ea6d0746e053aff7c7f201a3c"); // TODO REST_API_KEY 입력
            sb.append("&redirect_uri=http://localhost:3000/user/kakaoLogin"); // TODO 인가코드 받은 redirect_uri 입력
            sb.append("&prompt=login");
            sb.append("&code=" + authorization_code);
            bw.write(sb.toString());
            bw.flush();

            //결과 코드가 200이라면 성공
            int responseCode = connection.getResponseCode();
            log.debug("getKaKaoToken :: responseCode : {}", responseCode);
            if(responseCode == 401){
                //Error
            }

            //요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }

            //Gson 라이브러리에 포함된 클래스로 JSON파싱 객체 생성
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            log.info("access_token : {}", access_Token);
            log.info("refresh_token : {}", refresh_Token);

            //access-token을 파싱 하여 카카오 id가 디비에 있는지 확인
            HashMap<String, Object> userInfo = HttpUtil.parseToken(access_Token);
            Long userId = (Long) userInfo.get("user_id");
            //String nickname = userInfo.get("nickname").toString();
            log.info("parse result : {}", userId);

            if(userId == null){
                log.info("userId == null error");
                return ResponseEntity.status(401).body(null);
            }

            if(memberService.check_enroll_member(userId)){  // 회원가입해
                resultMap.put("access_token",access_Token);
                resultMap.put("refresh_token",refresh_Token);
                //resultMap.put("nickname", nickname);

                responseDto.setStatus(400);
                responseDto.setMessage("회원가입 정보 없음!!");
                responseDto.setData(resultMap);

                return new ResponseEntity<ResponseDto>(responseDto, HttpStatus.OK);  //200

            }else{  // 회원가입 되어 있어 그냥 token만 반환해
                Member member = memberService.findMember(userId);

                resultMap.put("nickname", member.getProfile().getNickname());
                resultMap.put("access_token", access_Token);
                resultMap.put("refresh_token", refresh_Token);

                responseDto.setStatus(200);
                responseDto.setMessage("로그인 완료!!");
                responseDto.setData(resultMap);
            }

            br.close();
            bw.close();
        }catch(IOException io){
            log.error(io.getMessage());

        }catch (UnauthorizedException uae){
            log.error(uae.getMessage());
            return ResponseEntity.status(401).body(null);
        }

        return ResponseEntity.ok().body(resultMap);
    }



//    @GetMapping("/logout")
//    public void logout(String accessToken){
//        String requestUrl = "https://kapi.kakao.com/v1/user/logout";
//
//        try{
//            URL url = new URL(requestUrl);
//
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
//
//            // 응답 코드
//            int responseCode = conn.getResponseCode();
//            System.out.println("responseCode =" + responseCode);
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//
//            String line = "";
//            String result = "";
//
//            while((line = br.readLine()) != null) {
//                result += line;
//            }
//            System.out.println("response body =" + result);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    @GetMapping("/logout")
    public void logout(){
        StringBuilder sb = new StringBuilder();
        sb.append("https://kauth.kakao.com/oauth/logout?");
        sb.append("client_id=" + "f0b916ceedccef620b4f4a6ab4e6bec5");
        sb.append("&logout_redirect_uri="+"http://localhost:3000/");

        String requestUrl = sb.toString();
        try{
            URL url = new URL(requestUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 응답 코드
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode =" + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            String result = "";

            while((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body =" + result);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody MemberJoinRequestDto memberJoinRequestDto){
        log.info("memberJoin Data : {}", memberJoinRequestDto);
        // token을 통해 userid 받아오기
        HashMap<String, Object> userInfo = HttpUtil.parseToken(memberJoinRequestDto.getAccess_token());

        // Service에 요청
        memberService.join(memberJoinRequestDto, (Long) userInfo.get("user_id"));

        return ResponseEntity.ok().body(null);
    }
}
