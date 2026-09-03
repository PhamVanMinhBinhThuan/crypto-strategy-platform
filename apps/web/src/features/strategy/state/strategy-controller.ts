export class StrategyMutationController {
  private pending = false;
  get isPending() {
    return this.pending;
  }
  async run<T>(mutation: () => Promise<T>, reconcile: () => Promise<void>): Promise<T | undefined> {
    if (this.pending) return;
    this.pending = true;
    try {
      return await mutation();
    } finally {
      try {
        await reconcile();
      } finally {
        this.pending = false;
      }
    }
  }
}
