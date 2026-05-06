import { Link } from 'react-router-dom'

export function NavTabs({
  locationPath,
  nav,
}: {
  locationPath: string
  nav: [string, string][]
}) {
  return (
    <div className="nav">
      {nav.map(([path, label]) => (
        <Link key={path} to={path} className={locationPath === path ? 'active' : ''}>{label}</Link>
      ))}
    </div>
  )
}

