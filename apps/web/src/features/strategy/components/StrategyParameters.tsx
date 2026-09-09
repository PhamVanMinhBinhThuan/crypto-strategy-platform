import type { StrategyDescriptor, UserStrategy } from "../model/strategy";

type ParameterValue = string | number | boolean;

const displayValue = (value: ParameterValue | undefined) => {
  if (value === undefined || value === "") return "Not configured";
  return String(value);
};

const descriptorFor = (
  systemStrategies: readonly StrategyDescriptor[],
  strategyId: string,
  version: string
) => systemStrategies.find((item) => item.strategyId === strategyId && item.version === version);

function ParameterList({
  descriptor,
  values
}: {
  descriptor?: StrategyDescriptor;
  values: Readonly<Record<string, ParameterValue>>;
}) {
  const names = Array.from(
    new Set([...(descriptor?.parameters.map((item) => item.name) ?? []), ...Object.keys(values)])
  );

  if (!names.length)
    return (
      <p className="strategy-parameter-empty">This strategy has no configurable parameters.</p>
    );

  return (
    <dl className="strategy-parameter-grid">
      {names.map((name) => {
        const field = descriptor?.parameters.find((item) => item.name === name);
        return (
          <div key={name}>
            <dt>
              <span>{name}</span>
              {field?.type ? <small>{field.type}</small> : null}
            </dt>
            <dd>{displayValue(values[name])}</dd>
            {field?.description ? <p>{field.description}</p> : null}
          </div>
        );
      })}
    </dl>
  );
}

export function StrategyParameters({
  descriptor,
  owned,
  systemStrategies
}: {
  descriptor?: StrategyDescriptor;
  owned?: UserStrategy;
  systemStrategies: readonly StrategyDescriptor[];
}) {
  if (!owned && descriptor)
    return (
      <section className="strategy-parameters" aria-label="Parameter definitions">
        <header>
          <div>
            <span className="strategy-section-kicker">Template parameters</span>
            <h2>{descriptor.displayName}</h2>
          </div>
          <span>{descriptor.parameters.length} fields</span>
        </header>
        <ParameterList
          descriptor={descriptor}
          values={Object.fromEntries(
            descriptor.parameters.map((field) => [field.name, field.defaultValue ?? ""])
          )}
        />
      </section>
    );

  if (!owned) return null;

  const source = owned.latestVersion.source;
  if (source.type === "SINGLE") {
    const system = descriptorFor(
      systemStrategies,
      source.strategy.strategyId,
      source.strategy.version
    );
    return (
      <section className="strategy-parameters" aria-label="Configured parameters">
        <header>
          <div>
            <span className="strategy-section-kicker">Latest configuration</span>
            <h2>{system?.displayName ?? source.strategy.strategyId}</h2>
          </div>
          <span>Version {owned.latestVersion.versionNo}</span>
        </header>
        <ParameterList descriptor={system} values={source.strategy.parameters} />
      </section>
    );
  }

  return (
    <section className="strategy-parameters" aria-label="Configured parameters">
      <header>
        <div>
          <span className="strategy-section-kicker">Composite configuration</span>
          <h2>{source.components.length} strategy components</h2>
        </div>
        <span>{source.policyId}</span>
      </header>
      <div className="strategy-component-parameters">
        {source.components.map((component) => {
          const system = descriptorFor(systemStrategies, component.strategyId, component.version);
          const weight = source.policyParameters[`weight.${component.strategyVersionId}`];
          return (
            <article key={component.strategyVersionId}>
              <header>
                <div>
                  <strong>{system?.displayName ?? component.strategyId}</strong>
                  <small>System version {component.version}</small>
                </div>
                {weight !== undefined ? <span>Weight {weight}</span> : <span>1 vote</span>}
              </header>
              <ParameterList descriptor={system} values={component.parameters} />
            </article>
          );
        })}
      </div>
    </section>
  );
}
