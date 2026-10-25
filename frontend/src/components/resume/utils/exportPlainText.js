/**
 * Converts structured resume data into clean ASCII plain text suitable for
 * raw ATS text pasting or saving as a standard .txt file.
 */
export const generatePlainTextResume = (data) => {
  if (!data) return '';

  const lines = [];

  // 1. Header
  lines.push((data.fullName || 'CANDIDATE NAME').toUpperCase());
  if (data.headline) lines.push(data.headline);
  
  const contacts = [
    data.email,
    data.phone,
    data.location,
    data.linkedinUrl,
    data.githubUrl,
    data.portfolioUrl
  ].filter(Boolean);

  if (contacts.length > 0) {
    lines.push(contacts.join(' | '));
  }
  lines.push('');

  // 2. Summary
  if (data.summary) {
    lines.push('PROFESSIONAL SUMMARY');
    lines.push('--------------------');
    lines.push(data.summary);
    lines.push('');
  }

  // 3. Technical Skills
  if (data.skills && data.skills.length > 0) {
    lines.push('TECHNICAL SKILLS');
    lines.push('----------------');
    data.skills.forEach((group) => {
      if (group.category && group.items && group.items.length > 0) {
        lines.push(`${group.category}: ${group.items.join(', ')}`);
      } else if (Array.isArray(group)) {
        lines.push(group.join(', '));
      }
    });
    lines.push('');
  }

  // 4. Experience
  if (data.experience && data.experience.length > 0) {
    lines.push('WORK EXPERIENCE');
    lines.push('---------------');
    data.experience.forEach((exp) => {
      const dates = `${exp.startDate || ''} - ${exp.isCurrent ? 'Present' : exp.endDate || ''}`.trim();
      const titleLine = [exp.title, exp.company].filter(Boolean).join(' - ');
      const subLine = [dates, exp.location].filter(Boolean).join(' | ');

      lines.push(titleLine);
      if (subLine) lines.push(subLine);

      if (exp.highlights && exp.highlights.length > 0) {
        exp.highlights.forEach((h) => {
          if (h && h.trim()) lines.push(`* ${h.trim()}`);
        });
      }
      lines.push('');
    });
  }

  // 5. Projects
  if (data.projects && data.projects.length > 0) {
    lines.push('PROJECTS');
    lines.push('--------');
    data.projects.forEach((proj) => {
      const header = [proj.title, proj.role].filter(Boolean).join(' - ');
      lines.push(header);
      if (proj.technologies) lines.push(`Technologies: ${proj.technologies}`);
      if (proj.linkUrl) lines.push(`Link: ${proj.linkUrl}`);

      if (proj.highlights && proj.highlights.length > 0) {
        proj.highlights.forEach((h) => {
          if (h && h.trim()) lines.push(`* ${h.trim()}`);
        });
      }
      lines.push('');
    });
  }

  // 6. Education
  if (data.education && data.education.length > 0) {
    lines.push('EDUCATION');
    lines.push('---------');
    data.education.forEach((edu) => {
      const degInst = [edu.degree, edu.institution].filter(Boolean).join(' - ');
      const details = [edu.graduationYear, edu.location].filter(Boolean).join(' | ');
      lines.push(degInst);
      if (details) lines.push(details);
      if (edu.gpa) lines.push(`GPA: ${edu.gpa}`);
      if (edu.honors) lines.push(`Honors: ${edu.honors}`);
      if (edu.coursework) lines.push(`Relevant Coursework: ${edu.coursework}`);
      lines.push('');
    });
  }

  // 7. Certifications
  if (data.certifications && data.certifications.length > 0) {
    lines.push('CERTIFICATIONS');
    lines.push('--------------');
    data.certifications.forEach((cert) => {
      const certLine = [cert.name, cert.issuer, cert.issueDate].filter(Boolean).join(' - ');
      lines.push(`* ${certLine}`);
    });
    lines.push('');
  }

  // 8. Achievements
  if (data.achievements && data.achievements.length > 0) {
    lines.push('ACHIEVEMENTS & AWARDS');
    lines.push('---------------------');
    data.achievements.forEach((ach) => {
      const title = [ach.title, ach.year].filter(Boolean).join(' - ');
      lines.push(`* ${title}`);
      if (ach.description) lines.push(`  ${ach.description}`);
    });
    lines.push('');
  }

  return lines.join('\n');
};

/**
 * Triggers browser download of the plain text resume.
 */
export const downloadPlainTextResume = (data, filename = 'resume.txt') => {
  const content = generatePlainTextResume(data);
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
