#!/bin/bash

echo "🚀 Starting migration from SpotBugs to JSpecify..."

# 1. Remove @NonNull imports entirely (swallows the newline)
echo "-> Removing @NonNull imports..."
find . -type f -name "*.java" -exec perl -pi -e 's/^import edu\.umd\.cs\.findbugs\.annotations\.NonNull;\r?\n//g' {} +

# 2. Remove @NonNull usages entirely (Handles standalone lines AND inline)
echo "-> Removing @NonNull annotations..."
# Pass A: Removes it if it's on its own line (eats leading indentation and the newline)
find . -type f -name "*.java" -exec perl -pi -e 's/^\s*\@NonNull\s*\r?\n//g' {} +
# Pass B: Removes it if it's inline (eats the annotation and the trailing space)
find . -type f -name "*.java" -exec perl -pi -e 's/\@NonNull\s+//g' {} +

# 3. Replace @Nullable imports in all Java files
echo "-> Replacing @Nullable imports..."
find . -type f -name "*.java" -exec perl -pi -e 's/import edu\.umd\.cs\.findbugs\.annotations\.Nullable;/import org.jspecify.annotations.Nullable;/g' {} +

# 4. Replace module-info.java requires directives
# Note: JSpecify's JPMS module name is exactly 'org.jspecify'
echo "-> Updating module-info.java files..."
find . -type f -name "module-info.java" -exec perl -pi -e 's/requires static com\.github\.spotbugs\.annotations;/requires static org.jspecify;/g' {} +

# 5. Update package-info.java files
echo "-> Updating package-info.java files..."
find . -type f -name "*.java" -exec perl -pi -e 's/\@edu\.umd\.cs\.findbugs\.annotations\.ReturnValuesAreNonnullByDefault/\@org.jspecify.annotations.NullMarked/g' {} +

echo "✅ Migration complete! Run 'git diff' to verify the changes."
