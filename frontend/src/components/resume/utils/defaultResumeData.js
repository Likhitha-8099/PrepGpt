export const INITIAL_RESUME_DATA = {
  // Contact & Header
  fullName: 'Alex Morgan',
  headline: 'Full Stack Software Engineer',
  email: 'alex.morgan@example.com',
  phone: '+1 (555) 234-5678',
  location: 'San Francisco, CA',
  linkedinUrl: 'https://linkedin.com/in/alexmorgan-dev',
  githubUrl: 'https://github.com/alexmorgan',
  portfolioUrl: 'https://alexmorgan.dev',

  // Professional Summary
  summary:
    'Versatile Full Stack Software Engineer with 3+ years of experience designing and operating high-throughput REST APIs, distributed caching layers, and responsive React web applications. Proven track record of improving system latency by 35% and accelerating release cycles with CI/CD automation.',

  // Skills categorized
  skills: [
    { category: 'Languages', items: ['Java', 'TypeScript', 'JavaScript', 'Python', 'SQL'] },
    { category: 'Frameworks & Libs', items: ['Spring Boot', 'React', 'Node.js', 'Express', 'Tailwind CSS'] },
    { category: 'Databases & Cloud', items: ['PostgreSQL', 'MySQL', 'Redis', 'AWS (S3, EC2)', 'Docker'] },
    { category: 'Tools & Practices', items: ['Git', 'JUnit', 'CI/CD Pipelines', 'RESTful APIs', 'Agile/Scrum'] }
  ],

  // Work Experience
  experience: [
    {
      id: 'exp-1',
      title: 'Software Engineer',
      company: 'Apex Cloud Solutions',
      location: 'San Francisco, CA',
      startDate: 'Aug 2022',
      endDate: 'Present',
      isCurrent: true,
      highlights: [
        'Architected and deployed microservices handling 2.5M daily requests with 99.98% uptime using Spring Boot and PostgreSQL.',
        'Engineered distributed Redis caching layer that reduced average API latency by 38% under peak load conditions.',
        'Built full-stack administrative dashboards using React, enabling non-technical stakeholders to monitor operational metrics in real time.',
        'Streamlined CI/CD deployment pipelines using GitHub Actions and Docker, reducing release verification time from 45 to 14 minutes.'
      ]
    },
    {
      id: 'exp-2',
      title: 'Junior Software Developer',
      company: 'Nexis Digital Labs',
      location: 'San Jose, CA',
      startDate: 'Jun 2021',
      endDate: 'Jul 2022',
      isCurrent: false,
      highlights: [
        'Developed reusable UI component library in React and TypeScript, adopted across 4 internal engineering pods.',
        'Designed secure authentication workflows leveraging JWT and OAuth2, eliminating unauthorized API access attempts.',
        'Wrote 120+ comprehensive unit and integration tests using JUnit and Mockito, raising test coverage from 68% to 89%.'
      ]
    }
  ],

  // Education
  education: [
    {
      id: 'edu-1',
      degree: 'B.S. in Computer Science',
      institution: 'University of California, Davis',
      location: 'Davis, CA',
      graduationYear: '2021',
      gpa: '3.82 / 4.0',
      honors: 'Magna Cum Laude, Dean’s Honor List',
      coursework: 'Data Structures, Operating Systems, Distributed Systems, Database Management'
    }
  ],

  // Projects
  projects: [
    {
      id: 'proj-1',
      title: 'Distributed Rate Limiter Service',
      role: 'Lead Developer',
      technologies: 'Java, Redis, Spring Boot, Docker',
      linkUrl: 'https://github.com/alexmorgan/distributed-rate-limiter',
      highlights: [
        'Implemented token-bucket and sliding-window rate limiting algorithms protecting internal endpoints against burst traffic.',
        'Benchmarked throughput up to 15,000 req/sec with sub-millisecond overhead using Redis pipelining.'
      ]
    },
    {
      id: 'proj-2',
      title: 'Collaborative Real-Time Code Canvas',
      role: 'Full Stack Creator',
      technologies: 'React, Node.js, WebSockets, Monaco Editor',
      linkUrl: 'https://github.com/alexmorgan/code-canvas',
      highlights: [
        'Engineered operational transformation engine enabling multi-user real-time code editing without conflicts.',
        'Constructed custom syntax highlighter and containerized code execution sandbox with 2-second timeout limits.'
      ]
    }
  ],

  // Certifications
  certifications: [
    {
      id: 'cert-1',
      name: 'AWS Certified Solutions Architect – Associate',
      issuer: 'Amazon Web Services',
      issueDate: '2023',
      credentialUrl: 'https://aws.amazon.com/verification'
    },
    {
      id: 'cert-2',
      name: 'Oracle Certified Professional: Java SE 17 Developer',
      issuer: 'Oracle',
      issueDate: '2022',
      credentialUrl: ''
    }
  ],

  // Achievements & Awards
  achievements: [
    {
      id: 'ach-1',
      title: 'First Place – CalHacks Collegiate Hackathon',
      issuer: 'UC Berkeley CalHacks',
      year: '2021',
      description: 'Built an AI-assisted accessibility tool for visually impaired programmers out of 350+ competing teams.'
    }
  ]
};

export const RESUME_TEMPLATES_METADATA = [
  {
    id: 'classic-professional',
    name: 'Classic Professional',
    subtitle: 'Traditional & Proven Single-Column ATS Standard',
    description:
      'Timeless single-column typography with clean horizontal section dividers and bold company headings. Highly recommended for corporate, finance, and enterprise engineering roles.',
    bestFor: 'Senior Engineers, Tech Leads, Traditional Enterprise & Corporate Tech',
    atsScore: 99,
    fontFamily: 'serif',
    accentColor: '#1e293b',
    tags: ['Traditional', 'Clean Dividers', 'High ATS Parse Rate', 'Conservative']
  },
  {
    id: 'modern-minimal',
    name: 'Modern Minimal',
    subtitle: 'Sleek Typography & Balanced Whitespace',
    description:
      'Contemporary sans-serif typography with restrained spacing, refined section headers, and optimized horizontal hierarchy. Clean and uncluttered for tech startups and modern product companies.',
    bestFor: 'Product Engineers, Full Stack Developers, Modern Startups',
    atsScore: 98,
    fontFamily: 'sans-serif',
    accentColor: '#4f46e5',
    tags: ['Modern Sans', 'Minimalist', 'Startups', 'High Readability']
  },
  {
    id: 'software-engineer',
    name: 'Software Engineer',
    subtitle: 'Technical Skills & Architecture First',
    description:
      'Engineered specifically for software developers. Positions categorized technical skills directly near the top, followed by featured portfolio projects with tech stacks and system metrics.',
    bestFor: 'Software Engineers, Backend/Frontend Developers, DevOps & Cloud Architects',
    atsScore: 99,
    fontFamily: 'sans-serif',
    accentColor: '#0284c7',
    tags: ['Tech First', 'Projects Focus', 'Categorized Stack', 'Developer Standard']
  },
  {
    id: 'graduate-fresher',
    name: 'Graduate / Fresher',
    subtitle: 'Academic Achievements & Hands-on Projects First',
    description:
      'Tailored for recent graduates, students, and early-career job seekers. Prominently highlights education, GPA/honors, coursework, and personal engineering projects ahead of work history.',
    bestFor: 'Fresh Graduates, Interns, Career Changers, Junior Developers',
    atsScore: 97,
    fontFamily: 'sans-serif',
    accentColor: '#059669',
    tags: ['Education First', 'Internships', 'Coursework', 'Junior Friendly']
  },
  {
    id: 'experienced-professional',
    name: 'Experienced Professional',
    subtitle: 'Executive Summary & Quantifiable Leadership Impact',
    description:
      'Designed for experienced engineers and engineering managers. Features an executive summary, core competencies matrix, and deep chronological work experience with quantifiable impact metrics.',
    bestFor: 'Staff/Principal Engineers, Engineering Managers, Architects (5+ Yrs Exp)',
    atsScore: 98,
    fontFamily: 'sans-serif',
    accentColor: '#7c3aed',
    tags: ['Executive', 'Core Competencies', 'Metrics Driven', 'Leadership']
  }
];
