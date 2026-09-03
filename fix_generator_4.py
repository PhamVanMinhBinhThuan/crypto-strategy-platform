import os

for root, _, files in os.walk('modules/experiment-execution'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            if 'generatorRef()' in content:
                content = content.replace('generatorRef()', 'generatorId()')
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
            
            # In SearchStartCommandFactoryService.java it might have generatorRef instead of generatorRef()
            if 'generatorRef' in content:
                content = content.replace('generatorRef', 'generatorId')
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)

print("Reverted generatorRef in experiment-execution.")
