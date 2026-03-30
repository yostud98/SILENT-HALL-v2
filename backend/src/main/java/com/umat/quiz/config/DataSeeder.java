package com.umat.quiz.config;

import com.umat.quiz.model.*;
import com.umat.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private StudentRepository studentRepo;
    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private QuizRepository quizRepo;
    @Autowired private QuestionRepository questionRepo;
    @Autowired private AnnouncementRepository announcementRepo;
    @Autowired private BCryptPasswordEncoder encoder;

    @Override
    public void run(String... args) {

        // ── Demo Students ─────────────────────────────────────
        if (studentRepo.count() == 0) {
            studentRepo.saveAll(List.of(
                s("UMAT/CS/21/0042","Ama Korantema","BSc Computer Science and Engineering","Computer Science","Computer Science",300,"ama.korantema@st.umat.edu.gh"),
                s("UMAT/CS/21/0043","Kojo Asante","BSc Computer Science and Engineering","Computer Science","Computer Science",300,"kojo.asante@st.umat.edu.gh"),
                s("UMAT/CS/21/0044","Yaw Darko","BSc Computer Science and Engineering","Computer Science","Computer Science",300,"yaw.darko@st.umat.edu.gh"),
                s("UMAT/ME/22/0010","Fatima Hassan","BSc Mining Engineering","Mining Engineering","Mining Engineering",200,"fatima.hassan@st.umat.edu.gh"),
                s("UMAT/EE/20/0055","Emeka Eze","BSc Electrical/Electronic Engineering","Electrical Engineering","Electrical Engineering",400,"emeka.eze@st.umat.edu.gh"),
                s("UMAT/CE/21/0031","Abena Mensah","BSc Civil Engineering","Civil Engineering","Civil Engineering",300,"abena.mensah@st.umat.edu.gh"),
                s("UMAT/GE/22/0018","Kwame Ofori","BSc Geomatic Engineering","Geomatic Engineering","Geomatic Engineering",200,"kwame.ofori@st.umat.edu.gh"),
                s("UMAT/MT/23/0007","Afia Sarpong","BSc Metallurgical Engineering","Metallurgical Engineering","Metallurgical Engineering",100,"afia.sarpong@st.umat.edu.gh")
            ));
            System.out.println("[UMAT Seeder] Demo students created.");
        }

        // ── Demo Lecturer ─────────────────────────────────────
        if (lecturerRepo.count() == 0) {
            Lecturer lec = new Lecturer();
            lec.setUsername("lecturer");
            lec.setPassword(encoder.encode("umat2026"));
            lec.setFullName("Dr. Emmanuel Boateng");
            lec.setDepartment("Computer Science");
            lec.setEmail("e.boateng@umat.edu.gh");
            lecturerRepo.save(lec);
            System.out.println("[UMAT Seeder] Demo lecturer: lecturer / umat2026");
        }

        // ── Demo Quiz ─────────────────────────────────────────
        if (quizRepo.count() == 0) {
            Quiz quiz = new Quiz();
            quiz.setTitle("Data Structures Mid-Semester Exam");
            quiz.setCourse("Computer Science");
            quiz.setLevel(300);
            quiz.setDurationMinutes(45);
            quiz.setScheduledStart(LocalDateTime.now().minusMinutes(5));
            quiz.setScheduledEnd(LocalDateTime.now().plusHours(2));
            quiz.setActive(true);
            quiz.setEnrollmentRequired(false); // open for demo
            quiz.setCreatedBy("Dr. Emmanuel Boateng");
            quiz.setDescription("This exam covers data structures from Week 1 to Week 8. Attempt all questions.");
            quizRepo.save(quiz);

            // Questions
            List<Question> qs = List.of(
                q(quiz,"What is the time complexity of binary search?","O(n)","O(log n)","O(n²)","O(1)","B",1),
                q(quiz,"Which data structure uses LIFO order?","Queue","Array","Stack","Linked List","C",1),
                q(quiz,"What does DFS stand for?","Data First Search","Depth First Search","Dynamic First Search","Direct Fast Search","B",1),
                q(quiz,"A complete binary tree with n nodes has height approximately:","n","n/2","log₂(n)","n²","C",2),
                q(quiz,"Which sorting algorithm has the best average-case time complexity?","Bubble Sort","Insertion Sort","Merge Sort","Selection Sort","C",2),
                q(quiz,"Which data structure is best for implementing a priority queue?","Array","Linked List","Heap","Stack","C",2),
                q(quiz,"What is the worst-case time complexity of QuickSort?","O(n log n)","O(n)","O(n²)","O(log n)","C",2),
                q(quiz,"In a hash table, what is a collision?","An error in the hash function","Two keys mapping to the same index","A full table","An empty slot","B",1)
            );
            questionRepo.saveAll(qs);
            System.out.println("[UMAT Seeder] Demo quiz with " + qs.size() + " questions created.");
        }

        // ── Demo Announcement ─────────────────────────────────
        if (announcementRepo.count() == 0) {
            Announcement ann = new Announcement();
            ann.setTitle("Important: Exam Instructions");
            ann.setBody("Please ensure your camera is working before starting the exam. Ensure you are in a quiet, well-lit environment. No additional materials are allowed. Good luck!");
            ann.setLecturerName("Dr. Emmanuel Boateng");
            ann.setTargetCourse("Computer Science");
            ann.setType(Announcement.AnnouncementType.QUIZ_INSTRUCTION);
            ann.setPinned(true);
            announcementRepo.save(ann);

            Announcement ann2 = new Announcement();
            ann2.setTitle("Welcome to the UMAT AI Quiz System");
            ann2.setBody("This system uses AI proctoring. Your session is recorded. Any malpractice will be flagged for review. Contact your lecturer if you experience technical issues.");
            ann2.setLecturerName("UMAT IT Department");
            ann2.setType(Announcement.AnnouncementType.GENERAL);
            ann2.setPinned(false);
            announcementRepo.save(ann2);
            System.out.println("[UMAT Seeder] Demo announcements created.");
        }
    }

    private Student s(String ref, String name, String program, String dept, String course, int level, String email) {
        Student s = new Student();
        s.setReferenceNumber(ref); s.setFullName(name);
        s.setProgram(program); s.setDepartment(dept);
        s.setCourse(course); s.setLevel(level); s.setEmail(email);
        return s;
    }

    private Question q(Quiz quiz, String text, String a, String b, String c, String d, String correct, int marks) {
        Question q = new Question();
        q.setQuiz(quiz); q.setQuestionText(text);
        q.setOptionA(a); q.setOptionB(b); q.setOptionC(c); q.setOptionD(d);
        q.setCorrectAnswer(correct); q.setMarks(marks);
        return q;
    }
}
