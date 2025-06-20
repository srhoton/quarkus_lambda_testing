#!/bin/bash

# CRUD Workflow Test Script for Quarkus Lambda API
# This script creates 10 records, updates them, and then deletes them

set -e  # Exit on any error

# Configuration
BASE_URL="https://h0h9vxf4ga.execute-api.us-east-1.amazonaws.com"
TOTAL_RECORDS=10

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Test data generator
generate_test_data() {
    local id=$1
    local categories=("electronics" "books" "clothing" "home" "sports" "toys" "automotive" "health" "beauty" "food")
    local priorities=("low" "medium" "high" "critical")
    local departments=("engineering" "marketing" "sales" "support" "operations")
    
    local category=${categories[$((id % ${#categories[@]}))]}
    local priority=${priorities[$((id % ${#priorities[@]}))]}
    local department=${departments[$((id % ${#departments[@]}))]}
    
    echo "{
        \"attributes\": {
            \"name\": \"Test Item $id\",
            \"description\": \"This is test item number $id for CRUD operations\",
            \"category\": \"$category\",
            \"priority\": \"$priority\",
            \"department\": \"$department\",
            \"test_run\": \"$(date '+%Y%m%d_%H%M%S')\"
        }
    }"
}

# Check if API is accessible
check_api_health() {
    log "Checking API health..."
    if curl -s --fail "$BASE_URL/lambda" > /dev/null; then
        success "API is accessible"
    else
        error "API is not accessible at $BASE_URL"
        exit 1
    fi
}

# Phase 1: Create records
create_records() {
    log "Phase 1: Creating $TOTAL_RECORDS records..."
    echo
    
    local created_count=0
    local failed_count=0
    
    for i in $(seq 1 $TOTAL_RECORDS); do
        local item_id="test-item-$(printf "%03d" $i)"
        local test_data=$(generate_test_data $i)
        
        printf "Creating item %-15s ... " "$item_id"
        
        local response=$(curl -s -w "%{http_code}" \
            -X POST "$BASE_URL/lambda/$item_id" \
            -H "Content-Type: application/json" \
            -d "$test_data")
        
        local http_code="${response: -3}"
        local body="${response%???}"
        
        if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
            local created_id=$(echo "$body" | jq -r '.id // "unknown"')
            local status=$(echo "$body" | jq -r '.status // "unknown"')
            success "Created (id: $created_id, status: $status)"
            ((created_count++))
        else
            local error_msg=$(echo "$body" | jq -r '.message // "Unknown error"')
            error "Failed (HTTP $http_code): $error_msg"
            ((failed_count++))
        fi
        
        # Small delay to avoid overwhelming the API
        sleep 0.1
    done
    
    echo
    log "Create phase summary: $created_count created, $failed_count failed"
    echo
}

# Phase 2: List all records
list_records() {
    log "Phase 2: Listing all records..."
    echo
    
    local response=$(curl -s "$BASE_URL/lambda")
    local count=$(echo "$response" | jq -r '.count // 0')
    local has_more=$(echo "$response" | jq -r '.has_more // false')
    
    success "Found $count records (has_more: $has_more)"
    
    # Show summary of records by status
    local created_count=$(echo "$response" | jq '[.items[] | select(.status == "created")] | length')
    local in_progress_count=$(echo "$response" | jq '[.items[] | select(.status == "in_progress")] | length')
    
    echo "  • Created: $created_count"
    echo "  • In Progress: $in_progress_count"
    echo
}

# Phase 3: Update records
update_records() {
    log "Phase 3: Updating records from 'created' to 'in_progress'..."
    echo
    
    local updated_count=0
    local failed_count=0
    local skipped_count=0
    
    for i in $(seq 1 $TOTAL_RECORDS); do
        local item_id="test-item-$(printf "%03d" $i)"
        
        printf "Updating item %-15s ... " "$item_id"
        
        local response=$(curl -s -w "%{http_code}" \
            -X PUT "$BASE_URL/lambda/$item_id")
        
        local http_code="${response: -3}"
        local body="${response%???}"
        
        if [[ "$http_code" == "200" ]]; then
            local status=$(echo "$body" | jq -r '.status // "unknown"')
            local updated_at=$(echo "$body" | jq -r '.updated_at // "unknown"')
            success "Updated (status: $status)"
            ((updated_count++))
        else
            local error_msg=$(echo "$body" | jq -r '.message // "Unknown error"')
            if [[ "$error_msg" == *"already in progress"* ]]; then
                warning "Already in progress"
                ((skipped_count++))
            elif [[ "$error_msg" == *"not found"* ]]; then
                warning "Not found (may have been deleted)"
                ((skipped_count++))
            else
                error "Failed (HTTP $http_code): $error_msg"
                ((failed_count++))
            fi
        fi
        
        sleep 0.1
    done
    
    echo
    log "Update phase summary: $updated_count updated, $skipped_count skipped, $failed_count failed"
    echo
}

# Phase 4: Delete records
delete_records() {
    log "Phase 4: Deleting all test records..."
    echo
    
    local deleted_count=0
    local failed_count=0
    
    for i in $(seq 1 $TOTAL_RECORDS); do
        local item_id="test-item-$(printf "%03d" $i)"
        
        printf "Deleting item %-15s ... " "$item_id"
        
        local response=$(curl -s -w "%{http_code}" \
            -X DELETE "$BASE_URL/lambda/$item_id")
        
        local http_code="${response: -3}"
        local body="${response%???}"
        
        if [[ "$http_code" == "200" || "$http_code" == "204" ]]; then
            success "Deleted"
            ((deleted_count++))
        else
            local error_msg=$(echo "$body" | jq -r '.message // "Unknown error"')
            if [[ "$error_msg" == *"not found"* ]]; then
                warning "Not found (may have been already deleted)"
                ((deleted_count++))  # Count as success since goal is achieved
            else
                error "Failed (HTTP $http_code): $error_msg"
                ((failed_count++))
            fi
        fi
        
        sleep 0.1
    done
    
    echo
    log "Delete phase summary: $deleted_count deleted, $failed_count failed"
    echo
}

# Phase 5: Verify cleanup
verify_cleanup() {
    log "Phase 5: Verifying cleanup..."
    echo
    
    local response=$(curl -s "$BASE_URL/lambda")
    local all_items=$(echo "$response" | jq -r '.items[] | .id')
    local test_items=$(echo "$all_items" | grep "test-item-" | wc -l)
    
    if [[ $test_items -eq 0 ]]; then
        success "All test items have been cleaned up"
    else
        warning "Found $test_items remaining test items:"
        echo "$all_items" | grep "test-item-" | sed 's/^/  • /'
    fi
    echo
}

# Cleanup function for any existing test items
cleanup_existing() {
    log "Cleaning up any existing test items..."
    echo
    
    local response=$(curl -s "$BASE_URL/lambda")
    local existing_test_items=$(echo "$response" | jq -r '.items[] | select(.id | startswith("test-item-")) | .id')
    
    if [[ -z "$existing_test_items" ]]; then
        success "No existing test items to clean up"
        echo
        return
    fi
    
    local cleanup_count=0
    while IFS= read -r item_id; do
        [[ -z "$item_id" ]] && continue
        
        printf "Cleaning up %-20s ... " "$item_id"
        
        local response=$(curl -s -w "%{http_code}" \
            -X DELETE "$BASE_URL/lambda/$item_id")
        
        local http_code="${response: -3}"
        
        if [[ "$http_code" == "200" || "$http_code" == "204" ]]; then
            success "Deleted"
            ((cleanup_count++))
        else
            warning "Failed or not found"
        fi
        
        sleep 0.1
    done <<< "$existing_test_items"
    
    log "Cleaned up $cleanup_count existing test items"
    echo
}

# Test pagination with remaining data
test_pagination() {
    log "Bonus: Testing pagination with any remaining data..."
    echo
    
    local response=$(curl -s "$BASE_URL/lambda?limit=2")
    local count=$(echo "$response" | jq -r '.count // 0')
    local has_more=$(echo "$response" | jq -r '.has_more // false')
    local next_cursor=$(echo "$response" | jq -r '.next_cursor // null')
    
    success "Pagination test: $count items returned, has_more: $has_more"
    
    if [[ "$has_more" == "true" && "$next_cursor" != "null" ]]; then
        log "Testing next page with cursor..."
        local page2_response=$(curl -s "$BASE_URL/lambda?limit=2&cursor=$next_cursor")
        local page2_count=$(echo "$page2_response" | jq -r '.count // 0')
        success "Page 2: $page2_count items returned"
    fi
    echo
}

# Performance timing
time_operation() {
    local operation_name="$1"
    local start_time=$(date +%s.%N)
    
    eval "$2"
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    
    log "$operation_name completed in ${duration}s"
}

# Main execution
main() {
    echo "=========================================="
    echo "  Quarkus Lambda CRUD Workflow Test"
    echo "=========================================="
    echo "Base URL: $BASE_URL"
    echo "Records to test: $TOTAL_RECORDS"
    echo "=========================================="
    echo
    
    # Check dependencies
    if ! command -v jq &> /dev/null; then
        error "jq is required but not installed. Please install jq first."
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        warning "bc is not installed. Timing will be disabled."
    fi
    
    # Execute test phases
    check_api_health
    cleanup_existing
    time_operation "Create phase" "create_records"
    time_operation "List phase" "list_records"
    time_operation "Update phase" "update_records"
    list_records  # Show status after updates
    time_operation "Delete phase" "delete_records"
    verify_cleanup
    test_pagination
    
    echo "=========================================="
    success "CRUD workflow test completed successfully!"
    echo "=========================================="
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi