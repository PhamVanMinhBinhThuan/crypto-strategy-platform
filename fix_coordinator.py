import re

path = 'apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),',
    'new com.cryptostrategy.platform.worker.infra.redis.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W2"),'
)
content = content.replace(
    '"01J7K8M9N0P1Q2R3S4T5A6V7W3",\n                        10,',
    'new com.cryptostrategy.platform.worker.infra.redis.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W3"),\n                        10,'
)
content = content.replace(
    '"01J7K8M9N0P1Q2R3S4T5A6V7W3",\n                3,',
    'new com.cryptostrategy.platform.experiment.api.ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),\n                3,'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed SearchCoordinatorTest")
