package com.conference.controller;

import com.conference.model.Paper;
import com.conference.service.ConferenceService;
import com.conference.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/author")
@RequiredArgsConstructor
public class AuthorController {
    
    private final PaperService paperService;
    private final ConferenceService conferenceService;

    @GetMapping("/papers/create")
    public String createPaper(Model model) {
        model.addAttribute("conferences", conferenceService.getAllConferences());
        return "author/create-paper";
    }

    @PostMapping("/papers/create")
    public String submitPaper(@RequestParam("title") String title,
                            @RequestParam("description") String description,
                            @RequestParam("conferenceId") Long conferenceId,
                            @RequestParam("paperFile") MultipartFile file,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/author/papers/create";
            }
            
            Paper paper = new Paper();
            paper.setTitle(title);
            paper.setDescription(description);
            paper.setFileName(file.getOriginalFilename());
            paper.setContentType(file.getContentType());
            paper.setData(file.getBytes());
            
            paperService.submitPaper(paper, authentication.getName(), conferenceId);
            redirectAttributes.addFlashAttribute("success", "Paper submitted successfully!");
            return "redirect:/author/papers/status";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error submitting paper: " + e.getMessage());
            return "redirect:/author/papers/create";
        }
    }

    @GetMapping("/papers/status")
    public String paperStatus(Authentication authentication, Model model) {
        model.addAttribute("papers", paperService.getPapersByAuthor(authentication.getName()));
        return "author/paper-status";
    }

    @GetMapping("/papers/{id}/download")
    public ResponseEntity<byte[]> downloadPaper(@PathVariable Long id) {
        Paper paper = paperService.getPaperById(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(paper.getContentType()));
        headers.setContentDispositionFormData("attachment", paper.getFileName());
        
        return new ResponseEntity<>(paper.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/papers/{id}")
    public String viewPaperDetails(@PathVariable Long id, Model model) {
        Paper paper = paperService.getPaperById(id);
        model.addAttribute("paper", paper);
        return "author/paper-details";
    }
}