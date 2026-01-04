import React, { useState, useEffect, useRef } from 'react';
import {
  IconButton,
  Box,
  Typography,
} from '@mui/material';
import {
  Close as CloseIcon,
  DragIndicator as DragIcon,
} from '@mui/icons-material';

interface ResizableModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  maxWidth?: string | number;
  maxHeight?: string | number;
  minWidth?: string | number;
  minHeight?: string | number;
  initialWidth?: string | number;
  initialHeight?: string | number;
  sidebarOpen?: boolean;
  actions?: React.ReactNode;
}

const ResizableModal: React.FC<ResizableModalProps> = ({
  open,
  onClose,
  title,
  children,
  maxWidth = '90vw',
  maxHeight = '90vh',
  minWidth = 400,
  minHeight = 300,
  initialWidth = '80vw',
  initialHeight = '80vh',
  sidebarOpen = true,
  actions,
}) => {
  const [dimensions, setDimensions] = useState({
    width: initialWidth,
    height: initialHeight,
  });
  const [isResizing, setIsResizing] = useState(false);
  
  const modalRef = useRef<HTMLDivElement>(null);
  const dragHandleRef = useRef<HTMLDivElement>(null);


  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isResizing) {
        // Calculate new height based on mouse position from bottom
        const newHeight = window.innerHeight - e.clientY;
        
        // Simple constraints
        const minH = 300; // 300px minimum
        const maxH = window.innerHeight * 0.8; // 80% of viewport height
        
        const constrainedHeight = Math.max(minH, Math.min(newHeight, maxH));
        
        setDimensions(prev => ({
          width: prev.width,
          height: constrainedHeight,
        }));
      }
    };

    const handleMouseUp = () => {
      setIsResizing(false);
    };

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isResizing, maxHeight, minHeight]);

  const handleDragStart = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // Start resizing from header
    setIsResizing(true);
  };

  const handleClose = () => {
    onClose();
  };

  if (!open) return null;

  return (
    <Box
      sx={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1300,
        display: 'flex',
        flexDirection: 'column',
        pointerEvents: 'none',
      }}
    >
       {/* Overlay - only covers the main content area, not sidebar */}
       <Box
         sx={{
           position: 'absolute',
           top: 0,
           left: sidebarOpen ? 240 : 64, // sidebar width or collapsed width (64px)
           right: 0,
           bottom: 0,
           backgroundColor: 'rgba(0, 0, 0, 0.3)',
           pointerEvents: 'auto',
         }}
         onClick={handleClose}
       />
       
       {/* Modal - positioned at bottom */}
       <Box
         ref={modalRef}
         sx={{
           position: 'absolute',
           left: sidebarOpen ? 240 : 64, // sidebar width or collapsed width (64px)
           right: 0,
           bottom: 0,
           height: dimensions.height,
           backgroundColor: 'background.paper',
           borderTopLeftRadius: 8,
           borderTopRightRadius: 8,
           boxShadow: '0 -4px 20px rgba(0, 0, 0, 0.15)',
           display: 'flex',
           flexDirection: 'column',
           overflow: 'hidden',
           minHeight: minHeight,
           maxHeight: maxHeight,
           pointerEvents: 'auto',
         }}
       >
         {/* Header */}
         <Box
           ref={dragHandleRef}
           sx={{
             display: 'flex',
             alignItems: 'center',
             justifyContent: 'space-between',
             p: 2,
             borderBottom: '1px solid #e0e0e0',
             backgroundColor: '#f5f5f5',
             cursor: 'ns-resize', // vertical resize cursor
             userSelect: 'none',
             '&:hover': {
               backgroundColor: '#eeeeee',
             },
           }}
           onMouseDown={handleDragStart}
         >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <DragIcon fontSize="small" color="action" />
            <Typography variant="h6" fontWeight="medium">
              {title}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {actions && (
              <Box sx={{ mr: 1 }}>
                {actions}
              </Box>
            )}
            <IconButton size="small" sx={{ p: 0.5 }}>
              <DragIcon fontSize="small" />
            </IconButton>
            <IconButton
              size="small"
              onClick={handleClose}
              sx={{
                '&:hover': {
                  backgroundColor: 'rgba(0, 0, 0, 0.1)',
                },
              }}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>

        {/* Content */}
        <Box
          sx={{
            flex: 1,
            overflow: 'auto',
            p: 0,
          }}
        >
          {children}
        </Box>

      </Box>
    </Box>
  );
};

export default ResizableModal;
