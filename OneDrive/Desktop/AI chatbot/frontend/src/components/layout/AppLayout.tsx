import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';

export const AppLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex bg-[#f8fafc] text-slate-800 font-sans">
      {/* 1. Left Dark Midnight Sidebar */}
      <Sidebar />

      {/* 2. Main Canvas */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        <Navbar />
        <main className="flex-1 overflow-y-auto px-4 sm:px-6 pb-6 pt-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
