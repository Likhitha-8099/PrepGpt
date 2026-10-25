import React from 'react';
import { NavLink } from 'react-router-dom';

const AdminNav = () => {
  const links = [
    { to: '/admin/dashboard', label: 'Overview' },
    { to: '/admin/users', label: 'Users' },
    { to: '/admin/questions', label: 'Questions' },
    { to: '/admin/sessions', label: 'Sessions' },
    { to: '/admin/audit-log', label: 'Audit Trail' }
  ];

  return (
    <div style={{ display: 'flex', gap: '0.375rem', flexWrap: 'wrap' }}>
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          className={({ isActive }) =>
            `btn btn-sm ${isActive ? 'btn-primary' : 'btn-secondary'}`
          }
        >
          {link.label}
        </NavLink>
      ))}
    </div>
  );
};

export default AdminNav;
