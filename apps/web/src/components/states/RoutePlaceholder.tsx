export function RoutePlaceholder({
  eyebrow,
  title,
  description
}: {
  eyebrow: string;
  title: string;
  description: string;
}) {
  return (
    <section className="placeholder">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p>{description}</p>
        <div className="status">
          <strong>Foundation ready</strong>
          <span>This screen is owned by a downstream UI feature.</span>
        </div>
      </div>
    </section>
  );
}
