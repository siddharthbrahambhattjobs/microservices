import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { startWith, map, debounceTime, distinctUntilChanged } from 'rxjs/operators';


interface Product {
  id: number;
  name: string;
  category: string;
  description: string;
}
@Component({
  selector: 'app-search',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './search.html',
  styleUrl: './search.scss',
})
export class Search {
  searchControl = new FormControl('', { nonNullable: true });

  products: Product[] = [
    { id: 1, name: 'Laptop Pro', category: 'Electronics', description: 'High performance laptop for office and development work.' },
    { id: 2, name: 'Laptop Air', category: 'Electronics', description: 'Lightweight laptop for daily productivity tasks.' },
    { id: 3, name: 'Wireless Mouse', category: 'Accessories', description: 'Ergonomic mouse with smooth wireless connectivity.' },
    { id: 4, name: 'Mechanical Keyboard', category: 'Accessories', description: 'Keyboard with tactile keys for fast typing.' },
    { id: 5, name: 'Office Chair', category: 'Furniture', description: 'Comfortable chair with lumbar support.' },
    { id: 6, name: 'Wooden Desk', category: 'Furniture', description: 'Spacious work desk for home office setup.' },
    { id: 7, name: 'Monitor 27 Inch', category: 'Electronics', description: 'Full HD monitor suitable for coding and design.' },
    { id: 8, name: 'USB-C Dock', category: 'Accessories', description: 'Docking station for laptop expansion and connectivity.' },
    { id: 9, name: 'Notebook Stand', category: 'Accessories', description: 'Stand for better posture and airflow.' },
    { id: 10, name: 'Desk Lamp', category: 'Furniture', description: 'LED lamp with adjustable brightness levels.' }
  ];

  filteredProducts: Product[] = [];
  searchText = '';
  showInitialMessage = true;
  showMinLengthMessage = true;

  constructor() {
    this.searchControl.valueChanges.pipe(
      startWith(''),
      map(value => value.replace(/\s+/g, ' ').trim()),
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchText = searchTerm;
      this.showInitialMessage = searchTerm.length === 0;
      this.showMinLengthMessage = searchTerm.length > 0 && searchTerm.length < 3;

      if (searchTerm.length < 3) {
        this.filteredProducts = [];
        return;
      }

      const term = searchTerm.toLowerCase();

      this.filteredProducts = this.products.filter(product =>
        product.name.toLowerCase().includes(term) ||
        product.category.toLowerCase().includes(term) ||
        product.description.toLowerCase().includes(term)
      );
    });
  }
}
