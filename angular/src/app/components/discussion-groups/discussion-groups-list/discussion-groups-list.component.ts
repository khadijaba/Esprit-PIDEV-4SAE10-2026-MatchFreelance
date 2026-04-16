import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DiscussionGroupService } from '../../../services/discussion-group.service';

@Component({
  selector: 'app-discussion-groups-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './discussion-groups-list.component.html',
  styleUrls: ['./discussion-groups-list.component.css']
})
export class DiscussionGroupsListComponent implements OnInit {
  groups: any[] = [];
  filteredGroups: any[] = [];
  popularGroups: any[] = [];
  searchQuery: string = '';
  selectedTopic: string = 'All';
  topics: string[] = ['All', 'Freelancing', 'Technology', 'Design', 'Business', 'Marketing', 'Other'];
  loading: boolean = false;

  constructor(
    private groupService: DiscussionGroupService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.loadPopularGroups();
  }

  loadGroups(): void {
    this.loading = true;
    this.groupService.getAllGroups().subscribe({
      next: (groups) => {
        this.groups = groups;
        this.filteredGroups = groups;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading groups:', error);
        this.loading = false;
      }
    });
  }

  loadPopularGroups(): void {
    this.groupService.getPopularGroups().subscribe({
      next: (groups) => {
        this.popularGroups = groups.slice(0, 5);
      },
      error: (error) => {
        console.error('Error loading popular groups:', error);
      }
    });
  }

  searchGroups(): void {
    if (this.searchQuery.trim()) {
      this.groupService.searchGroups(this.searchQuery).subscribe({
        next: (groups) => {
          this.filteredGroups = groups;
        },
        error: (error) => {
          console.error('Error searching groups:', error);
        }
      });
    } else {
      this.filterByTopic();
    }
  }

  filterByTopic(): void {
    if (this.selectedTopic === 'All') {
      this.filteredGroups = this.groups;
    } else {
      this.filteredGroups = this.groups.filter(g => g.topic === this.selectedTopic);
    }
  }

  viewGroup(groupId: number): void {
    this.router.navigate(['/groups', groupId]);
  }

  createGroup(): void {
    this.router.navigate(['/groups/create']);
  }

  getDefaultLogo(): string {
    return 'https://via.placeholder.com/100?text=Group';
  }
}
