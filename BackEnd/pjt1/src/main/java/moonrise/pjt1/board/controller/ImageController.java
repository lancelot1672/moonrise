package moonrise.pjt1.board.controller;

import lombok.RequiredArgsConstructor;
import moonrise.pjt1.board.service.FileService;
import moonrise.pjt1.commons.response.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Null;
import java.util.List;

@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {
    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<?> imageUpload(MultipartFile[] multipartFileList){
        ResponseDto responseDto = fileService.upload(multipartFileList);

        return new ResponseEntity<ResponseDto>(responseDto, HttpStatus.OK);
    }
}
