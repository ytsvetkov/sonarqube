require 'database_version'

def call_upgrade_and_start
  DatabaseVersion.upgrade_and_start
end