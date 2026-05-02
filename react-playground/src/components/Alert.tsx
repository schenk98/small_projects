import React from 'react'

interface Props{
  children: React.ReactNode;
  onClose: () => void;
}

const Alert = ({children, onClose}: Props) => {
  return (
    <div className="alert alert-primary alert-dismissible fade show" role="alert">{children}
    <button type="button" className="close-btn" data-dismiss="alert" aria-label="Close" onClick={onClose}>
      <span aria-hidden="true">&times;</span>
    </button>
  </div>
  )
}

export default Alert