export class LatestRequest {
  private generation = 0;
  private controller?: AbortController;
  next() {
    this.controller?.abort();
    this.controller = new AbortController();
    return { generation: ++this.generation, signal: this.controller.signal } as const;
  }
  isLatest(generation: number) {
    return generation === this.generation;
  }
  cancel() {
    this.controller?.abort();
  }
}
