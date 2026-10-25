import React from 'react';
import ClassicTemplate from './templates/ClassicTemplate';
import ModernTemplate from './templates/ModernTemplate';
import SoftwareEngineerTemplate from './templates/SoftwareEngineerTemplate';
import GraduateFresherTemplate from './templates/GraduateFresherTemplate';
import ExperiencedTemplate from './templates/ExperiencedTemplate';

const ResumePreviewRenderer = ({ templateId, resumeData }) => {
  switch (templateId) {
    case 'modern-minimal':
      return <ModernTemplate data={resumeData} />;
    case 'software-engineer':
      return <SoftwareEngineerTemplate data={resumeData} />;
    case 'graduate-fresher':
      return <GraduateFresherTemplate data={resumeData} />;
    case 'experienced-professional':
      return <ExperiencedTemplate data={resumeData} />;
    case 'classic-professional':
    default:
      return <ClassicTemplate data={resumeData} />;
  }
};

export default ResumePreviewRenderer;
