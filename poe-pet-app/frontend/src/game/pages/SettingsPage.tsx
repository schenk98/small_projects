import { Link } from 'react-router-dom'
import type { Tokens } from '../../auth/AuthScreens'

export function SettingsPage({
  setTokens,
}: {
  setTokens: (t: Tokens | null) => void
}) {
  return (
    <div className="card">
      <h3>Account</h3>
      <p><Link to="/forgot-password">Reset password</Link></p>
      <button type="button" onClick={() => setTokens(null)}>Logout</button>
      <h3 id="developer-tools">Developer tools</h3>
      <p>
        The <strong>Developer</strong> dropdown in the top bar (grant coins, refill stats, set stat %) is hidden unless your account is flagged as privileged.
      </p>
      <ul>
        <li>
          <strong>MongoDB:</strong> in database <code>poe_pet</code>, collection <code>users</code>, set <code>privileged: true</code> on your user document (same <code>_id</code> as JWT subject), then restart nothing — just refresh the app after the next dashboard load.
        </li>
        <li>
          <strong>Or env / config:</strong> add your login email to <code>APP_PRIVILEGED_EMAILS</code> (comma-separated) or <code>app.privilegedEmails</code> in <code>backend/src/main/resources/application.yml</code>, then restart the Spring Boot server.
        </li>
      </ul>
    </div>
  )
}

