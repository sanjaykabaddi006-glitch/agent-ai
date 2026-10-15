import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
  MessageSquare,
  BookOpen,
  FileText,
  FolderKanban,
  Cpu,
  History,
  Users,
  Database,
  ChevronDown,
  Sparkles,
  Bot
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  const navItems: Array<{
    name: string;
    path: string;
    icon: React.ComponentType<{ className?: string }>;
    badge: string | null;
    badgeColor?: string;
  }> = [
    { name: 'Chat', path: '/chat', icon: MessageSquare, badge: null },
    { name: 'Knowledge Explorer', path: '/okf', icon: BookOpen, badge: null },
    { name: 'Documents', path: '/documents', icon: FileText, badge: null },
    { name: 'Conversation History', path: '/chat', icon: History, badge: null },
  ];

  const initials = user?.fullName
    ? user.fullName
        .split(' ')
        .map((n) => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : 'JD';

  return (
    <aside className="w-[260px] bg-[#0b1021] text-slate-300 flex flex-col justify-between shrink-0 h-screen sticky top-0 border-r border-[#1a2342] select-none z-30">
      {/* 1. Brand Logo Header */}
      <div className="p-5 pb-3">
        <Link to="/" className="flex items-center space-x-3 group">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-cyan-400 p-[1.5px] shadow-lg shadow-blue-500/20 group-hover:scale-105 transition-transform shrink-0">
            <div className="w-full h-full bg-[#0b1021] rounded-[14px] flex items-center justify-center">
              <Bot className="w-5 h-5 text-blue-400" />
            </div>
          </div>
          <div className="min-w-0">
            <div className="flex items-center space-x-1.5">
              <span className="font-extrabold text-white text-[15px] tracking-tight truncate">
                NeuraKnow
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium truncate">
              Enterprise Knowledge Assistant
            </p>
          </div>
        </Link>
      </div>

      {/* 2. Navigation Items */}
      <div className="flex-1 px-3.5 py-2 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.name + item.path}
              to={item.name === 'Conversation History' ? '/chat?view=history' : item.path}
              state={item.name === 'Conversation History' ? { openHistory: true } : undefined}
              className={({ isActive }) =>
                `group relative flex items-center justify-between px-3.5 py-2.5 rounded-2xl font-medium text-xs transition-all duration-200 ${
                  isActive
                    ? 'bg-[#2d5cf6] text-white shadow-lg shadow-blue-600/30 font-semibold'
                    : 'text-slate-400 hover:text-white hover:bg-[#131b35]/80'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <div className="flex items-center space-x-3 truncate">
                    <Icon
                      className={`w-4 h-4 shrink-0 transition-colors ${
                        isActive ? 'text-white' : 'text-slate-400 group-hover:text-blue-400'
                      }`}
                    />
                    <span className="truncate">{item.name}</span>
                  </div>

                  {item.badge && (
                    <span
                      className={`w-5 h-5 rounded-full text-[10px] font-bold flex items-center justify-center shrink-0 ${
                        item.badgeColor || 'bg-blue-500/20 text-blue-300'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </>
              )}
            </NavLink>
          );
        })}
      </div>

      {/* 3. Glowing Wave Graphic & Lower Section */}
      <div className="relative pt-2 px-3.5 pb-3">
        {/* Decorative Wave Illustration */}
        <div className="relative w-full h-16 mb-2 overflow-hidden rounded-2xl pointer-events-none opacity-85">
          <svg
            viewBox="0 0 240 64"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            className="w-full h-full object-cover"
          >
            <path
              d="M0 45C40 20 80 60 120 35C160 10 200 50 240 25V64H0V45Z"
              fill="url(#wave-grad-1)"
              fillOpacity="0.3"
            />
            <path
              d="M0 35C50 55 90 15 140 40C190 65 210 20 240 38"
              stroke="url(#line-grad-1)"
              strokeWidth="2"
              strokeLinecap="round"
            />
            <path
              d="M0 45C60 20 100 55 160 25C200 5 220 40 240 30"
              stroke="url(#line-grad-2)"
              strokeWidth="1.5"
              strokeDasharray="3 3"
            />
            <defs>
              <linearGradient id="wave-grad-1" x1="0" y1="20" x2="240" y2="60" gradientUnits="userSpaceOnUse">
                <stop stopColor="#4f46e5" />
                <stop offset="1" stopColor="#06b6d4" />
              </linearGradient>
              <linearGradient id="line-grad-1" x1="0" y1="30" x2="240" y2="40" gradientUnits="userSpaceOnUse">
                <stop stopColor="#818cf8" />
                <stop offset="0.5" stopColor="#c084fc" />
                <stop offset="1" stopColor="#38bdf8" />
              </linearGradient>
              <linearGradient id="line-grad-2" x1="0" y1="30" x2="240" y2="30" gradientUnits="userSpaceOnUse">
                <stop stopColor="#3b82f6" />
                <stop offset="1" stopColor="#a855f7" />
              </linearGradient>
            </defs>
          </svg>
        </div>

        {/* Knowledge Base Status Card is hidden from UI per request while preserving full backend access */}


        {/* User Profile Bar */}
        <div className="flex items-center justify-between p-2 rounded-2xl hover:bg-[#131b35]/80 transition-colors border border-transparent hover:border-slate-800 cursor-pointer">
          <div className="flex items-center space-x-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-[#2d5cf6] text-white flex items-center justify-center text-xs font-bold shrink-0 shadow-md shadow-blue-500/30">
              {initials}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-white truncate leading-tight">
                {user?.fullName || 'John Doe'}
              </p>
              <p className="text-[10px] text-slate-400 truncate leading-tight">
                Enterprise Admin
              </p>
            </div>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 shrink-0" />
        </div>
      </div>
    </aside>
  );
};
