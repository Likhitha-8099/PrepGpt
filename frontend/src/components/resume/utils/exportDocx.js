/**
 * Exports resume data to a Word-compatible document (.doc / .docx)
 * using standard Microsoft Office XML/HTML format.
 * Compatible with Microsoft Word, Google Docs, and LibreOffice.
 */
export const downloadWordResume = (data, templateName = 'Classic Professional', filename = 'resume.doc') => {
  if (!data) return;

  const escapeHtml = (str) => {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const contactList = [
    data.email,
    data.phone,
    data.location,
    data.linkedinUrl ? `<a href="${escapeHtml(data.linkedinUrl)}">LinkedIn</a>` : null,
    data.githubUrl ? `<a href="${escapeHtml(data.githubUrl)}">GitHub</a>` : null,
    data.portfolioUrl ? `<a href="${escapeHtml(data.portfolioUrl)}">Portfolio</a>` : null
  ].filter(Boolean);

  let htmlBody = `
    <div style="font-family: Arial, Helvetica, sans-serif; font-size: 11pt; line-height: 1.4; color: #111827;">
      <!-- Header -->
      <div style="text-align: center; margin-bottom: 14pt; border-bottom: 2pt solid #1e293b; padding-bottom: 6pt;">
        <h1 style="font-size: 20pt; font-weight: bold; margin: 0 0 4pt 0; text-transform: uppercase; color: #0f172a;">
          ${escapeHtml(data.fullName || 'Candidate Name')}
        </h1>
        ${data.headline ? `<div style="font-size: 12pt; font-weight: 600; color: #475569; margin-bottom: 4pt;">${escapeHtml(data.headline)}</div>` : ''}
        <div style="font-size: 9.5pt; color: #334155;">
          ${contactList.join(' &nbsp;|&nbsp; ')}
        </div>
      </div>
  `;

  // Summary
  if (data.summary) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Professional Summary
        </h2>
        <p style="margin: 0; font-size: 10.5pt; text-align: justify;">
          ${escapeHtml(data.summary)}
        </p>
      </div>
    `;
  }

  // Skills
  if (data.skills && data.skills.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Technical Skills
        </h2>
        <div style="font-size: 10pt;">
    `;
    data.skills.forEach((group) => {
      if (group.category && group.items && group.items.length > 0) {
        htmlBody += `<p style="margin: 2pt 0;"><strong>${escapeHtml(group.category)}:</strong> ${escapeHtml(group.items.join(', '))}</p>`;
      }
    });
    htmlBody += `
        </div>
      </div>
    `;
  }

  // Work Experience
  if (data.experience && data.experience.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Work Experience
        </h2>
    `;
    data.experience.forEach((exp) => {
      const dates = `${exp.startDate || ''} – ${exp.isCurrent ? 'Present' : exp.endDate || ''}`.trim();
      htmlBody += `
        <div style="margin-bottom: 8pt; page-break-inside: avoid;">
          <table style="width: 100%; border-collapse: collapse; margin-bottom: 2pt;">
            <tr>
              <td style="font-size: 10.5pt; font-weight: bold; color: #0f172a;">
                ${escapeHtml(exp.title || '')} <span style="font-weight: normal; color: #475569;">— ${escapeHtml(exp.company || '')}</span>
              </td>
              <td style="font-size: 9.5pt; text-align: right; color: #475569; font-weight: 500;">
                ${escapeHtml(dates)} ${exp.location ? `| ${escapeHtml(exp.location)}` : ''}
              </td>
            </tr>
          </table>
      `;

      if (exp.highlights && exp.highlights.length > 0) {
        htmlBody += `<ul style="margin: 2pt 0 4pt 18pt; padding: 0; font-size: 10pt;">`;
        exp.highlights.forEach((h) => {
          if (h && h.trim()) {
            htmlBody += `<li style="margin-bottom: 2pt;">${escapeHtml(h.trim())}</li>`;
          }
        });
        htmlBody += `</ul>`;
      }
      htmlBody += `</div>`;
    });
    htmlBody += `</div>`;
  }

  // Projects
  if (data.projects && data.projects.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Technical Projects
        </h2>
    `;
    data.projects.forEach((proj) => {
      htmlBody += `
        <div style="margin-bottom: 8pt; page-break-inside: avoid;">
          <table style="width: 100%; border-collapse: collapse; margin-bottom: 2pt;">
            <tr>
              <td style="font-size: 10.5pt; font-weight: bold; color: #0f172a;">
                ${escapeHtml(proj.title || '')} ${proj.role ? `<span style="font-weight: normal; color: #475569;">(${escapeHtml(proj.role)})</span>` : ''}
              </td>
              <td style="font-size: 9.5pt; text-align: right; color: #0284c7;">
                ${proj.linkUrl ? `<a href="${escapeHtml(proj.linkUrl)}">Project Link</a>` : ''}
              </td>
            </tr>
          </table>
          ${proj.technologies ? `<div style="font-size: 9.5pt; color: #475569; margin-bottom: 2pt;"><strong>Stack:</strong> ${escapeHtml(proj.technologies)}</div>` : ''}
      `;

      if (proj.highlights && proj.highlights.length > 0) {
        htmlBody += `<ul style="margin: 2pt 0 4pt 18pt; padding: 0; font-size: 10pt;">`;
        proj.highlights.forEach((h) => {
          if (h && h.trim()) {
            htmlBody += `<li style="margin-bottom: 2pt;">${escapeHtml(h.trim())}</li>`;
          }
        });
        htmlBody += `</ul>`;
      }
      htmlBody += `</div>`;
    });
    htmlBody += `</div>`;
  }

  // Education
  if (data.education && data.education.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Education
        </h2>
    `;
    data.education.forEach((edu) => {
      htmlBody += `
        <div style="margin-bottom: 6pt; page-break-inside: avoid;">
          <table style="width: 100%; border-collapse: collapse;">
            <tr>
              <td style="font-size: 10.5pt; font-weight: bold; color: #0f172a;">
                ${escapeHtml(edu.degree || '')} — <span style="font-weight: normal;">${escapeHtml(edu.institution || '')}</span>
              </td>
              <td style="font-size: 9.5pt; text-align: right; color: #475569;">
                ${escapeHtml(edu.graduationYear || '')} ${edu.location ? `| ${escapeHtml(edu.location)}` : ''}
              </td>
            </tr>
          </table>
          ${edu.gpa ? `<div style="font-size: 9.5pt; color: #334155;"><strong>GPA:</strong> ${escapeHtml(edu.gpa)} ${edu.honors ? `(${escapeHtml(edu.honors)})` : ''}</div>` : ''}
          ${edu.coursework ? `<div style="font-size: 9.5pt; color: #475569;"><strong>Coursework:</strong> ${escapeHtml(edu.coursework)}</div>` : ''}
        </div>
      `;
    });
    htmlBody += `</div>`;
  }

  // Certifications
  if (data.certifications && data.certifications.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Certifications
        </h2>
        <ul style="margin: 2pt 0 4pt 18pt; padding: 0; font-size: 10pt;">
    `;
    data.certifications.forEach((cert) => {
      const line = [cert.name, cert.issuer, cert.issueDate].filter(Boolean).join(' — ');
      htmlBody += `<li style="margin-bottom: 2pt;">${escapeHtml(line)}</li>`;
    });
    htmlBody += `
        </ul>
      </div>
    `;
  }

  // Achievements
  if (data.achievements && data.achievements.length > 0) {
    htmlBody += `
      <div style="margin-bottom: 12pt;">
        <h2 style="font-size: 12pt; font-weight: bold; border-bottom: 1pt solid #cbd5e1; padding-bottom: 2pt; margin: 0 0 6pt 0; text-transform: uppercase; color: #1e293b;">
          Achievements & Honors
        </h2>
        <ul style="margin: 2pt 0 4pt 18pt; padding: 0; font-size: 10pt;">
    `;
    data.achievements.forEach((ach) => {
      htmlBody += `<li style="margin-bottom: 2pt;"><strong>${escapeHtml(ach.title)}</strong> ${ach.year ? `(${escapeHtml(ach.year)})` : ''}${ach.description ? `: ${escapeHtml(ach.description)}` : ''}</li>`;
    });
    htmlBody += `
        </ul>
      </div>
    `;
  }

  htmlBody += `</div>`;

  // Complete Word document envelope with proper XML declarations and margin setup
  const docxTemplate = `
    <html xmlns:o='urn:schemas-microsoft-com:office:office'
          xmlns:w='urn:schemas-microsoft-com:office:word'
          xmlns='http://www.w3.org/TR/REC-html40'>
      <head>
        <meta charset='utf-8'>
        <title>${escapeHtml(data.fullName || 'Resume')}</title>
        <!--[if gte mso 9]>
        <xml>
          <w:WordDocument>
            <w:View>Print</w:View>
            <w:Zoom>100</w:Zoom>
            <w:DoNotOptimizeForBrowser/>
          </w:WordDocument>
        </xml>
        <![endif]-->
        <style>
          @page {
            size: 8.5in 11in;
            margin: 0.6in 0.6in 0.6in 0.6in;
            mso-header-margin: 0.3in;
            mso-footer-margin: 0.3in;
            mso-paper-source: 0;
          }
          body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
          }
          a {
            color: #2563eb;
            text-decoration: none;
          }
        </style>
      </head>
      <body>
        ${htmlBody}
      </body>
    </html>
  `;

  const blob = new Blob(['\ufeff', docxTemplate], {
    type: 'application/msword;charset=utf-8'
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
