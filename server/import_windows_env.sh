#!/bin/bash
# Import Windows environment variables

# Get Windows environment variables and clean them
powershell.exe -c "Get-ChildItem Env: | Where-Object { \$_.Name -match '^[A-Za-z_][A-Za-z0-9_]*$' } | ForEach-Object { 'export ' + \$_.Name + '=\"' + \$_.Value + '\"' }" > temp_env.sh

# Source the variables
source temp_env.sh

# Clean up
rm temp_env.sh

echo "Windows environment variables imported to current shell"
echo "Common variables available:"
echo "COMPUTERNAME: $COMPUTERNAME"
echo "USERNAME: $USERNAME" 
echo "USERPROFILE: $USERPROFILE"
