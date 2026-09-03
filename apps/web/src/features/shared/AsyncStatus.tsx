export function AsyncStatus({ message, urgent = false }: { message?: string; urgent?: boolean }) {
  return (
    <p
      className="sr-only"
      role={urgent ? "alert" : "status"}
      aria-live={urgent ? "assertive" : "polite"}
      aria-atomic="true"
    >
      {message}
    </p>
  );
}
