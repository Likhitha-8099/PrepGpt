package com.prepgpt.config;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.InterviewCategory;
import com.prepgpt.entity.Question;
import com.prepgpt.entity.QuestionType;
import com.prepgpt.repository.InterviewCategoryRepository;
import com.prepgpt.repository.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class QuestionSeeder {

    private final QuestionRepository questionRepository;
    private final InterviewCategoryRepository categoryRepository;

    public QuestionSeeder(QuestionRepository questionRepository, InterviewCategoryRepository categoryRepository) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
    }

    public void seedQuestions() {
        seedCategoryQuestions("Technical Interview", QuestionType.TECHNICAL, getTechnicalQuestions());
        seedCategoryQuestions("DSA Interview", QuestionType.DSA, getDsaQuestions());
        seedCategoryQuestions("Coding Interview", QuestionType.CODING, getCodingQuestions());
        seedCategoryQuestions("HR Interview", QuestionType.HR, getHrQuestions());
        seedCategoryQuestions("Behavioral Interview", QuestionType.BEHAVIORAL, getBehavioralQuestions());
        seedCategoryQuestions("Project Interview", QuestionType.PROJECT, getProjectQuestions());
        seedCategoryQuestions("Resume Interview", QuestionType.RESUME, getResumeQuestions());
        seedCategoryQuestions("Job Description Interview", QuestionType.JOB_DESCRIPTION, getJobDescriptionQuestions());
    }

    private void seedCategoryQuestions(String categoryName, QuestionType type, List<QuestionSeedData> questionsData) {
        Optional<InterviewCategory> categoryOpt = categoryRepository.findByName(categoryName);
        if (categoryOpt.isEmpty()) return;

        InterviewCategory category = categoryOpt.get();
        for (QuestionSeedData q : questionsData) {
            if (!questionRepository.existsByCategoryIdAndQuestionText(category.getId(), q.text)) {
                questionRepository.save(new Question(
                        category,
                        q.text,
                        type,
                        q.difficulty,
                        q.guidance
                ));
            }
        }
    }

    private static class QuestionSeedData {
        String text;
        Difficulty difficulty;
        String guidance;

        QuestionSeedData(String text, Difficulty difficulty, String guidance) {
            this.text = text;
            this.difficulty = difficulty;
            this.guidance = guidance;
        }
    }

    // 1. Technical Questions (15)
    private List<QuestionSeedData> getTechnicalQuestions() {
        return List.of(
                new QuestionSeedData("What is object-oriented programming and what are its core pillars?", Difficulty.EASY, "Explain encapsulation, inheritance, polymorphism, and abstraction."),
                new QuestionSeedData("Explain inheritance in Java and the use of super keyword.", Difficulty.EASY, "Discuss class hierarchies, method overriding, and code reuse."),
                new QuestionSeedData("What is polymorphism? Explain compile-time vs runtime polymorphism.", Difficulty.EASY, "Cover method overloading (compile-time) and method overriding (runtime)."),
                new QuestionSeedData("What is encapsulation and why is data hiding important?", Difficulty.EASY, "Explain private access modifiers, getter/setter patterns, and invariant protection."),
                new QuestionSeedData("What are Java Optional classes and how do they prevent NullPointerExceptions?", Difficulty.EASY, "Discuss functional API usage with ofNullable, map, flatMap, orElse, and best practices."),
                
                new QuestionSeedData("What is the difference between an interface and an abstract class in Java?", Difficulty.MEDIUM, "Discuss multiple inheritance, default methods, fields, and design intent."),
                new QuestionSeedData("Explain Java memory management: Stack vs Heap allocation.", Difficulty.MEDIUM, "Discuss primitives and stack frames vs object allocation, garbage collection references."),
                new QuestionSeedData("How does HashMap work internally in Java 8+?", Difficulty.MEDIUM, "Explain hashing, buckets, initial capacity, load factor, collision handling, and LinkedList to Red-Black Tree conversion."),
                new QuestionSeedData("Explain Java reflection API and annotations processing.", Difficulty.MEDIUM, "Discuss runtime introspection, inspecting metadata, and framework usage like Spring."),
                new QuestionSeedData("What is the difference between fail-fast and fail-safe iterators in Java Collections?", Difficulty.MEDIUM, "Discuss ConcurrentModificationException, modCount, and ConcurrentHashMap iterators."),
                
                new QuestionSeedData("Explain the Garbage Collection process in Java and generational GC zones.", Difficulty.HARD, "Discuss Young Generation (Eden, Survivor) vs Tenured/Old Gen and GC algorithms like G1/ZGC."),
                new QuestionSeedData("Explain thread safety, synchronization, and volatile keyword in Java multithreading.", Difficulty.HARD, "Discuss memory visibility guarantees, synchronized blocks, atomic locks, and race conditions."),
                new QuestionSeedData("How does Java Happens-Before memory model guarantee thread visibility and reordering prevention?", Difficulty.HARD, "Discuss memory barriers, volatile read/write semantics, and JMM specs."),
                new QuestionSeedData("Explain ClassLoaders hierarchy and delegation model in Java JVM.", Difficulty.HARD, "Discuss Bootstrap, Extension/Platform, and Application classloaders with parent delegation."),
                new QuestionSeedData("What are Virtual Threads (Project Loom) in modern Java and how do they differ from OS platform threads?", Difficulty.HARD, "Discuss lightweight threads, continuation stack mounting, and I/O non-blocking execution.")
        );
    }

    // 2. DSA Questions (10+)
    private List<QuestionSeedData> getDsaQuestions() {
        return List.of(
                new QuestionSeedData("What is the difference between an array and a linked list in terms of memory layout and access time?", Difficulty.EASY, "Discuss contiguous memory vs pointer links and O(1) random access vs O(N) sequential search."),
                new QuestionSeedData("Explain the Binary Search algorithm and state its time and space complexity.", Difficulty.EASY, "Discuss divide-and-conquer strategy on sorted data with O(log N) runtime."),
                new QuestionSeedData("What is the time and space complexity of Merge Sort vs Quick Sort?", Difficulty.MEDIUM, "Explain O(N log N) average/worst case complexities, stability, and auxiliary memory usage."),
                new QuestionSeedData("Explain Stack and Queue data structures and their primary applications.", Difficulty.EASY, "Discuss LIFO vs FIFO operations and usages in call stacks, recursion, and breadth-first search queues."),
                new QuestionSeedData("What is a Binary Search Tree (BST) and how does searching operate?", Difficulty.EASY, "Explain left-child < root < right-child invariant and average O(log N) vs O(N) worst case height."),
                new QuestionSeedData("Explain BFS (Breadth-First Search) vs DFS (Depth-First Search) graph traversals.", Difficulty.MEDIUM, "Discuss Queue vs Stack implementation, memory consumption, and shortest-path applications."),
                new QuestionSeedData("What is Hashing and how are hash collisions resolved in hash tables?", Difficulty.MEDIUM, "Explain hash functions, uniform distribution, chaining vs open addressing."),
                new QuestionSeedData("What is Dynamic Programming? Explain memoization vs tabulation with an example.", Difficulty.HARD, "Discuss optimal substructure, overlapping subproblems, top-down vs bottom-up approaches."),
                new QuestionSeedData("Explain the Two-Pointer technique and when it is applicable.", Difficulty.MEDIUM, "Discuss sorted array searches, sliding windows, and reducing O(N^2) loops to O(N)."),
                new QuestionSeedData("What is an AVL Tree and how do single/double rotations maintain balance?", Difficulty.HARD, "Explain height balance factor bounds (-1, 0, 1) and LL, RR, LR, RL balancing rotations.")
        );
    }

    // 3. Coding Questions (10+)
    private List<QuestionSeedData> getCodingQuestions() {
        return List.of(
                new QuestionSeedData("How would you write a function to reverse a string without using built-in reverse methods?", Difficulty.EASY, "Discuss two-pointer char array swapping or string building."),
                new QuestionSeedData("Explain how to find the two numbers in an array that add up to a target sum (Two Sum problem).", Difficulty.EASY, "Discuss brute-force O(N^2) vs single-pass HashMap O(N) lookup approach."),
                new QuestionSeedData("How do you detect a cycle in a singly linked list (Floyd's Cycle Detection algorithm)?", Difficulty.MEDIUM, "Explain slow (1 step) and fast (2 steps) pointer collision detection."),
                new QuestionSeedData("Write logic to find the longest substring without repeating characters.", Difficulty.MEDIUM, "Discuss sliding window technique with set/map tracking character indices."),
                new QuestionSeedData("How do you check if a string of brackets ()[]{} is valid and balanced?", Difficulty.EASY, "Explain Stack push for open brackets and pop matching verification for closing brackets."),
                new QuestionSeedData("How would you merge two sorted arrays without extra memory space if one array has enough capacity at the end?", Difficulty.MEDIUM, "Explain three-pointer backwards iteration starting from end indices."),
                new QuestionSeedData("Explain how to find the maximum subarray sum (Kadane's Algorithm).", Difficulty.MEDIUM, "Discuss dynamic state tracking max current sum vs global max sum in O(N)."),
                new QuestionSeedData("How would you implement an LRU (Least Recently Used) Cache with O(1) get and put operations?", Difficulty.HARD, "Explain Doubly LinkedList + HashMap combination."),
                new QuestionSeedData("Write an algorithm to search a target in a 2D matrix where rows and columns are sorted.", Difficulty.MEDIUM, "Discuss top-right corner elimination search in O(M + N)."),
                new QuestionSeedData("How do you solve the Trapping Rain Water problem given an elevation map?", Difficulty.HARD, "Discuss two-pointer height bounds vs precomputed left/right max arrays.")
        );
    }

    // 4. HR Questions (10+)
    private List<QuestionSeedData> getHrQuestions() {
        return List.of(
                new QuestionSeedData("Tell me about yourself, your background, and why you are pursuing this career.", Difficulty.EASY, "Provide structured elevator pitch highlighting education, key projects, technical passions, and goal alignment."),
                new QuestionSeedData("Why do you want to join our organization specifically?", Difficulty.EASY, "Show research on company mission, culture, products, and alignment with personal growth."),
                new QuestionSeedData("What are your primary technical strengths?", Difficulty.EASY, "Highlight core competencies with practical project evidence."),
                new QuestionSeedData("What is one professional weakness you have identified and how are you addressing it?", Difficulty.EASY, "Discuss genuine growth areas with proactive steps taken for improvement."),
                new QuestionSeedData("Where do you see yourself professionally five years from now?", Difficulty.MEDIUM, "Show career aspiration, continuous learning mindset, and leadership/technical mastery goals."),
                new QuestionSeedData("Why should our team hire you over other qualified candidates?", Difficulty.MEDIUM, "Highlight combination of technical skills, problem-solving drive, and enthusiasm."),
                new QuestionSeedData("How do you handle high-pressure deadlines or sudden project requirements changes?", Difficulty.MEDIUM, "Discuss prioritization, clear communication, breaking down tasks, and staying calm."),
                new QuestionSeedData("Describe your ideal work environment and team culture.", Difficulty.EASY, "Focus on collaboration, mentorship, constructive feedback, and growth mindset."),
                new QuestionSeedData("Why did you select computer science / software engineering as your specialization?", Difficulty.EASY, "Share genuine story of passion for technology and building solutions."),
                new QuestionSeedData("Are you open to relocating or working in flexible shift schedules if required?", Difficulty.EASY, "Provide transparent response regarding availability and flexibility.")
        );
    }

    // 5. Behavioral Questions (10+)
    private List<QuestionSeedData> getBehavioralQuestions() {
        return List.of(
                new QuestionSeedData("Describe a situation where you had a technical disagreement with a team member. How did you resolve it?", Difficulty.MEDIUM, "Use STAR method: situation, technical task, data-backed discussion, and collaborative resolution."),
                new QuestionSeedData("Give an example of a time when a project or assignment did not go as planned. What did you learn?", Difficulty.MEDIUM, "Discuss resilience, root-cause analysis, and adaptive strategy."),
                new QuestionSeedData("Describe a time when you had to learn a completely new technology under a tight deadline.", Difficulty.EASY, "Highlight learning strategies, documentation research, prototyping, and goal delivery."),
                new QuestionSeedData("Tell me about a complex problem you solved independently. What was your step-by-step approach?", Difficulty.MEDIUM, "Discuss problem decomposition, isolation, hypothesis testing, and solution verification."),
                new QuestionSeedData("Describe a scenario where you took initiative to improve a system or process without being asked.", Difficulty.HARD, "Highlight leadership, proactive ownership, technical quality, and measurable impact."),
                new QuestionSeedData("Give an example of how you prioritized multiple competing assignments when deadlines overlapped.", Difficulty.EASY, "Discuss priority matrix, stakeholder communication, and time blocking."),
                new QuestionSeedData("Tell me about a time you mentored or helped a peer understand a complex technical topic.", Difficulty.EASY, "Discuss empathy, communication clarity, breaking down abstractions, and patience."),
                new QuestionSeedData("Describe a scenario where you received critical performance feedback. How did you react and adjust?", Difficulty.MEDIUM, "Show maturity, openness to feedback, action plan implementation, and outcome."),
                new QuestionSeedData("Give an example of how you handled working with a challenging or difficult teammate.", Difficulty.MEDIUM, "Discuss active listening, professional empathy, alignment on common goals, and clear documentation."),
                new QuestionSeedData("Tell me about a project you are most proud of completing. What was your specific contribution?", Difficulty.EASY, "Detail technical role, engineering choices, challenges overcome, and final result.")
        );
    }

    // 6. Project Questions (10+)
    private List<QuestionSeedData> getProjectQuestions() {
        return List.of(
                new QuestionSeedData("Walk me through the architecture of your primary major project.", Difficulty.EASY, "Explain multi-tier architecture, frontend/backend separation, database design, and key modules."),
                new QuestionSeedData("Why did you select the specific tech stack (frameworks, database, programming language) for your project?", Difficulty.MEDIUM, "Provide justification comparing alternatives, evaluation criteria, developer productivity, and scalability."),
                new QuestionSeedData("What was the single most challenging technical bug or bottleneck you encountered in your project?", Difficulty.MEDIUM, "Explain bug symptoms, diagnostic steps, profiling/logging tools used, and the eventual resolution."),
                new QuestionSeedData("How did you design your database schema, indexes, and relationships for data integrity?", Difficulty.MEDIUM, "Discuss normalization, primary/foreign keys, indexing strategies, and query performance."),
                new QuestionSeedData("How did you secure your project API endpoints and handle user authentication/authorization?", Difficulty.MEDIUM, "Discuss JWT tokens, password hashing (BCrypt), session management, and CORS policies."),
                new QuestionSeedData("What measures did you take to test, validate, and verify the correctness of your project modules?", Difficulty.EASY, "Discuss unit testing, API testing (Postman), edge case validation, and end-to-end integration tests."),
                new QuestionSeedData("If you had another two months to work on your project, what features or optimizations would you implement next?", Difficulty.EASY, "Discuss performance caching, microservices splitting, CI/CD automated deployment, and scalable infrastructure."),
                new QuestionSeedData("How does your application handle unexpected system failures, database disconnections, or invalid input data?", Difficulty.HARD, "Discuss exception handling layers, transaction rollback, graceful degradation, and user-friendly error responses."),
                new QuestionSeedData("What design patterns (e.g., Singleton, Factory, Repository, MVC) did you implement in your project code?", Difficulty.MEDIUM, "Detail structural, creational, and behavioral design patterns used in the codebase."),
                new QuestionSeedData("How did you handle concurrent user requests or state synchronization in your application?", Difficulty.HARD, "Discuss stateless architecture, database locking, atomic operations, and thread pool scaling.")
        );
    }

    // 7. Resume Questions (10+)
    private List<QuestionSeedData> getResumeQuestions() {
        return List.of(
                new QuestionSeedData("You mentioned Spring Boot in your technical skills. Explain how Spring Dependency Injection and IoC container work.", Difficulty.EASY, "Discuss Inversion of Control, Bean lifecycle, @Autowired, Component scanning, and application context."),
                new QuestionSeedData("Your resume lists experience with MySQL. Explain transaction ACID properties and isolation levels.", Difficulty.MEDIUM, "Discuss Atomicity, Consistency, Isolation, Durability, and Read Committed vs Repeatable Read."),
                new QuestionSeedData("You listed React.js as a core frontend skill. Explain Virtual DOM, Reconciliation, and React Hooks lifecycle.", Difficulty.MEDIUM, "Discuss diffing algorithm, state updates, useEffect dependency arrays, and component re-rendering."),
                new QuestionSeedData("Can you elaborate on the project achievements and metrics highlighted in your resume?", Difficulty.EASY, "Provide context around numerical results, efficiency gains, user impact, and technical execution."),
                new QuestionSeedData("You listed Java 17 as a proficiency. What are some key features introduced in modern Java versions (e.g. Records, Sealed Classes, Pattern Matching)?", Difficulty.MEDIUM, "Discuss functional features, memory optimization, and syntax improvements."),
                new QuestionSeedData("Your resume mentions RESTful API design. What are the key principles and HTTP status codes for RESTful services?", Difficulty.EASY, "Discuss statelessness, standard methods (GET, POST, PUT, DELETE), resource URI naming, and 2xx/4xx/5xx status codes."),
                new QuestionSeedData("Explain the Git workflow and version control strategies you referenced on your resume.", Difficulty.EASY, "Discuss branching strategies (feature branches), pull requests, merge conflict resolution, and commits."),
                new QuestionSeedData("You mentioned Data Structures & Algorithms proficiency. How do you analyze time and space complexity using Big-O notation?", Difficulty.EASY, "Discuss asymptotic analysis, worst/average/best cases, and common complexity classes."),
                new QuestionSeedData("Can you explain the educational coursework and core CS subjects you completed?", Difficulty.EASY, "Summarize Operating Systems, DBMS, Computer Networks, Software Engineering, and OOP fundamentals."),
                new QuestionSeedData("Your resume lists experience with Maven/Gradle. Explain build lifecycle phases and dependency management.", Difficulty.EASY, "Discuss compile, test, package, install phases, transitive dependencies, and pom.xml configuration.")
        );
    }

    // 8. Job Description Questions (10+)
    private List<QuestionSeedData> getJobDescriptionQuestions() {
        return List.of(
                new QuestionSeedData("The job description emphasizes building scalable backend REST APIs. How do you design APIs for high throughput and low latency?", Difficulty.MEDIUM, "Discuss caching strategies (Redis), asynchronous processing, database indexing, and connection pooling."),
                new QuestionSeedData("The role requires proficiency in relational databases and SQL optimization. How do you analyze slow queries and optimize execution plans?", Difficulty.MEDIUM, "Discuss EXPLAIN plans, index usage, avoiding SELECT *, avoiding N+1 query problems, and join optimization."),
                new QuestionSeedData("The job description requires clean code principles and unit testing. What is Test-Driven Development (TDD) and how do you write maintainable code?", Difficulty.EASY, "Discuss Red-Green-Refactor cycle, SOLID principles, mocking dependencies, and code coverage."),
                new QuestionSeedData("This position requires working in an Agile development environment. Describe your experience with Sprints, Standups, and Jira/Kanban.", Difficulty.EASY, "Discuss daily standups, sprint planning, story points, backlog grooming, and retrospective meetings."),
                new QuestionSeedData("The job requires experience with microservices architecture or modular design. What are the advantages and challenges of microservices over monolithic architectures?", Difficulty.HARD, "Discuss independent deployment, technological flexibility vs distributed system complexity, network latency, and service discovery."),
                new QuestionSeedData("The role mentions containerization and Cloud deployments (e.g., Docker, AWS/GCP). What are the benefits of Docker containers?", Difficulty.MEDIUM, "Discuss environment consistency, isolation, lightweight virtualization, and deployment reproducibility."),
                new QuestionSeedData("The job description highlights problem-solving and troubleshooting in production environments. How do you diagnose production issues?", Difficulty.MEDIUM, "Discuss centralized logging, error monitoring, metrics telemetry, thread dumps, and systematic isolation."),
                new QuestionSeedData("This role involves cross-functional collaboration with product managers, QA, and frontend teams. How do you ensure clear alignment?", Difficulty.EASY, "Discuss clear API contract documentation (Swagger/OpenAPI), early alignment, and regular syncs."),
                new QuestionSeedData("The job requires continuous integration and continuous deployment (CI/CD). Describe a standard automated pipeline.", Difficulty.MEDIUM, "Discuss code checkout, automated linting/testing, artifact build, container image creation, and deployment stages."),
                new QuestionSeedData("The position values security best practices in software development. How do you prevent OWASP Top 10 vulnerabilities like SQL Injection and XSS?", Difficulty.HARD, "Discuss parameterized queries, input validation/sanitization, output encoding, HTTPS, and secure headers.")
        );
    }
}
